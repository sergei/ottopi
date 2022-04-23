package com.santacruzinstruments.ottopi.control;

import static java.lang.Thread.sleep;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import timber.log.Timber;

public class UsbReader {

    private final PendingIntent permissionIntent;

    public interface UsbConnectionListener {
        void OnConnectionStatus(boolean connected);
        void onDataReceived(byte[]  buff, int size);
    }

    private static final String ACTION_USB_PERMISSION =
            "com.santacruzinstruments.ottopi.USB_PERMISSION";

    private final UsbManager usbManager;
    private boolean bKeepRunning = true;

    private UsbAccessory activeAccessory;
    private UsbAccessory permissionForAccessory;
    private UsbAccessory connectionWithAccessory;
    private boolean accessoryConnected = false;
    private boolean permissionGranted = false;
    private boolean accessoryDisconnected = false;

    ParcelFileDescriptor fileDescriptor;
    FileInputStream inputStream;

    FileOutputStream outputStream;
    private final byte [] writeusbdata = new byte[256];
    private static final int BUFFER_SIZE = 4096;

    enum UsbState {
        DISCOVERING,
        PERMISSION_PENDING,
        OPENING,
        READING
    }

    private UsbState usbState = UsbState.DISCOVERING;

    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Timber.d("USB Broadcast receiver got intent action %s", action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(accessory != null){
                        Timber.d("Permission granted for accessory %s", accessory.getDescription());
                        UsbReader.this.permissionForAccessory = accessory;
                        UsbReader.this.permissionGranted = true;
                    }else{
                        Timber.d("Hmm, permission granted, but no accessory is specified");
                    }
                }
                else {
                    Timber.d( "permission denied for accessory %s ", accessory);
                }
            }else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null) {
                    Timber.d("Attached accessory %s", accessory.getDescription());
                    UsbReader.this.connectionWithAccessory = accessory;
                    UsbReader.this.accessoryConnected = true;
                }else{
                    Timber.d("Hmm, accessory attached, but null is specified");
                }
            }else  if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)){
                Timber.d("Accessory detached");
                accessoryDisconnected = true;
            }
        }
    };

    private final UsbConnectionListener usbConnectionListener;

    UsbReader(Context context, UsbConnectionListener usbConnectionListener){
        this.usbConnectionListener = usbConnectionListener;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter);

    }

    public synchronized void stop(){
        bKeepRunning = false;
    }

    @SuppressWarnings("BusyWait")
    void run(){
        Timber.d("Starting USB thread");

        byte[] buffer = new byte[BUFFER_SIZE];

        while (bKeepRunning){

            if( accessoryDisconnected ){
                accessoryDisconnected = false;
                Timber.d("USB accessory disconnected, closing it");
                closeAccessory();
                usbState = UsbState.DISCOVERING;
            }

            switch (usbState){
                case DISCOVERING:
                    // Accessory discovery
                    // Poll USB manager for accessory list
                    UsbAccessory accessory = findAccessory();
                    // Check if it was found in broadcast receiver
                    if ( accessoryConnected ){
                        accessoryConnected = false;
                        accessory = connectionWithAccessory;
                    }

                    if( accessory != null) {
                        Timber.d("USB Accessory %s discovered", accessory.getDescription());
                        // Check if we have permission to use it
                        if ( usbManager.hasPermission(accessory)){
                            usbState = UsbState.OPENING;
                            activeAccessory = accessory;
                        }else{
                            usbManager.requestPermission(accessory, permissionIntent);
                            usbState = UsbState.PERMISSION_PENDING;
                        }
                    }
                    try {sleep(1000);} catch (InterruptedException ignore) {}
                    break;
                case PERMISSION_PENDING:
                    if (permissionGranted ){
                        permissionGranted = false;
                        usbState = UsbState.OPENING;
                        activeAccessory = permissionForAccessory;
                    }else{
                        try {sleep(1000);} catch (InterruptedException ignore) {}
                    }
                    break;
                case OPENING:
                    if ( openAccessory(activeAccessory) ){
                        boolean ok = SetConfig(4800,(byte)8,(byte)1,(byte)0,(byte)0);
                        if ( ok ) {
                            usbState = UsbState.READING;
                            usbConnectionListener.OnConnectionStatus(true);
                        }else{
                            Timber.d("Failed to configure USB accessory, closing it");
                            closeAccessory();
                            try {sleep(1000);} catch (InterruptedException ignore) {}
                            usbState = UsbState.DISCOVERING;
                        }

                    }else{
                        try {sleep(1000);} catch (InterruptedException ignore) {}
                        activeAccessory = null;
                        usbState = UsbState.DISCOVERING;
                    }

                    break;
                case READING:
                    try {
                        int readCount = inputStream.read(buffer,0,BUFFER_SIZE);
                        usbConnectionListener.onDataReceived(buffer, readCount);
                    } catch (IOException e) {
                        Timber.d("Failed to read from USB accessory, closing it %s", e.getMessage());
                        closeAccessory();
                        try {sleep(1000);} catch (InterruptedException ignore) {}
                        usbState = UsbState.DISCOVERING;
                    }
                    break;
            }

        }

        Timber.d("USB thread finished");
        if ( activeAccessory != null){
            closeAccessory();
        }

    }

    private synchronized UsbAccessory findAccessory() {
        UsbAccessory [] accessories = usbManager.getAccessoryList();
        if ( accessories != null) {
            Timber.d("Found accessory %s", accessories[0]);
            return accessories[0];
        }else{
            return null;
        }
    }

    // The call from manager thread
    public synchronized void setAccessory(UsbAccessory accessory) {
        Timber.d("setAccessory() called with: accessory = [" + accessory + "]");
        this.connectionWithAccessory = accessory;
        this.accessoryConnected = true;
    }

    private synchronized void closeAccessory() {
        Timber.d("Closing accessory");

        if( inputStream != null){
            try {
                inputStream.close();
            } catch (IOException e) {
                Timber.e("Failed to close accessory input stream  %s", e.getMessage());
            }
        }else{
            Timber.e("accessory input stream is null");
        }

        if( outputStream != null){
            try {
                outputStream.close();
            } catch (IOException e) {
                Timber.e("Failed to close accessory output stream  %s", e.getMessage());
            }
        }else{
            Timber.e("accessory output stream is null");
        }

        if ( fileDescriptor != null){
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                Timber.e("Failed to close accessory file descriptor %s", e.getMessage());
            }
        }else{
            Timber.e("accessory file descriptor is null");
        }

        fileDescriptor = null;
        outputStream = null;
        inputStream = null;

        activeAccessory = null;

        usbConnectionListener.OnConnectionStatus(false);
    }

    private boolean openAccessory(UsbAccessory accessory) {
        Timber.d("Opening accessory %s", accessory.getDescription());
        try {
            fileDescriptor = usbManager.openAccessory(accessory);
            if (fileDescriptor != null) {
                FileDescriptor fd = fileDescriptor.getFileDescriptor();
                inputStream = new FileInputStream(fd);
                outputStream = new FileOutputStream(fd);
                Timber.d("Accessory opened");
                return true;
            }else{
                Timber.e("Failed to open accessory");
            }
        } catch (java.lang.SecurityException e) {
            Timber.e("Can not open accessory due to lack of permissions");
        }catch (IllegalArgumentException e){
            Timber.e("Can not open accessory %s", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean SendPacket(int numBytes)
    {
        boolean success = false;
        try {
            if(outputStream != null){
                Timber.v("Sending %d bytes to USB", numBytes);
                outputStream.write(writeusbdata, 0,numBytes);
                success = true;
            }else{
                Timber.v("No open stream to send %d bytes to USB", numBytes);
            }
        } catch (IOException e) {
            Timber.e("Error writing to USB Accessory: %s", e.getMessage());
        }
        return success;
    }

    public boolean SetConfig(int baud, byte dataBits, byte stopBits,
                          byte parity, byte flowControl)
    {
        Timber.d("SetConfig() called with: baud = [" + baud + "], dataBits = [" + dataBits + "], stopBits = [" + stopBits + "], parity = [" + parity + "], flowControl = [" + flowControl + "]");

        /*prepare the baud rate buffer*/
        writeusbdata[0] = (byte)baud;
        writeusbdata[1] = (byte)(baud >> 8);
        writeusbdata[2] = (byte)(baud >> 16);
        writeusbdata[3] = (byte)(baud >> 24);

        /*data bits*/
        writeusbdata[4] = dataBits;
        /*stop bits*/
        writeusbdata[5] = stopBits;
        /*parity*/
        writeusbdata[6] = parity;
        /*flow control*/
        writeusbdata[7] = flowControl;

        /*send the UART configuration packet*/
        return SendPacket((int)8);
    }

}
