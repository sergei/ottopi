package com.santacruzinstruments.ottopi.control.canbus;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class UsbAccessoryTransport implements CanBusWriter, SlipPacket.SlipListener, SlipPacket.SlipWriter {

    private final PendingIntent permissionIntent;

    private static final String ACTION_USB_PERMISSION =
            "com.santacruzinstruments.ottopi.USB_PERMISSION";
    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

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
                        UsbAccessoryTransport.this.permissionForAccessory = accessory;
                        UsbAccessoryTransport.this.permissionGranted = true;
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
                    UsbAccessoryTransport.this.connectionWithAccessory = accessory;
                    UsbAccessoryTransport.this.accessoryConnected = true;
                }else{
                    Timber.d("Hmm, accessory attached, but null is specified");
                }
            }else  if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)){
                Timber.d("Accessory detached");
                accessoryDisconnected = true;
            }
        }
    };

    private final N2KConnectionListener connectionListener;

    SlipPacket slipPacket = new SlipPacket(this, this);

    public UsbAccessoryTransport(Context context, N2KConnectionListener usbConnectionListener){
        this.connectionListener = usbConnectionListener;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter);

    }

    public synchronized void stop(){
        bKeepRunning = false;
    }

    @SuppressWarnings("BusyWait")
    public void run(){
        Timber.d("Starting USB thread");

        byte[] buffer = new byte[BUFFER_SIZE];

        long lastTickMs = System.currentTimeMillis();
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
                        boolean ok = SetConfig(115200,(byte)8,(byte)1,(byte)0,(byte)0);
                        if ( ok ) {
                            usbState = UsbState.READING;
                            connectionListener.OnConnectionStatus(true);
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
                        for ( int i=0; i<readCount; i++) {
                            slipPacket.onSlipByteReceived(buffer[i]);
                        }
                    } catch (IOException e) {
                        Timber.d("Failed to read from USB accessory, closing it %s", e.getMessage());
                        closeAccessory();
                        try {sleep(1000);} catch (InterruptedException ignore) {}
                        usbState = UsbState.DISCOVERING;
                    }
                    break;
            }
            long now = System.currentTimeMillis();
            if( now - lastTickMs > 1000){
                lastTickMs = now;
                connectionListener.onTick();
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

        connectionListener.OnConnectionStatus(false);
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

    @Override
    public void onPacketReceived(byte[] packet, int l) {
        int can_id;
        byte[] data;
        // can_id is first four bytes of packet
        can_id = (packet[0] & 0xff) << 24 |
                (packet[1] & 0xff) << 16 |
                (packet[2] & 0xff) << 8 |
                (packet[3] & 0xff);
        // len is the fifth byte of packet
        int len = packet[4] & 0xff;
        // data is the rest of the packet
        data = new byte[len];
        System.arraycopy(packet, 5, data, 0, len);
        String ydnuMsg = SerialUsbTransport.formatYdnuRawString(can_id, data);
        Timber.d("USBA_N2K,%d,[%s R %s]", len,
                TIME_FORMAT.format(new Date()),
                ydnuMsg.substring(0, ydnuMsg.length()-2));
        connectionListener.onFrameReceived(can_id, data);
    }

    @Override
    public void sendCanFrame(int canAddr, byte[] data) {
        byte[] buffer = new byte[BUFFER_SIZE];
        // First four bytes off buffer is canAddr in network byte order
        buffer[0] = (byte) ((canAddr >> 24) & 0xff);
        buffer[1] = (byte) ((canAddr >> 16) & 0xff);
        buffer[2] = (byte) ((canAddr >> 8) & 0xff);
        buffer[3] = (byte) (canAddr & 0xff);
        // Then one byte of data length
        buffer[4] = (byte) (data.length & 0xff);
        // Then the data
        System.arraycopy(data, 0, buffer, 5, data.length);
        int len = data.length + 5;
        slipPacket.encodeAndSendPacket(buffer, len);
    }

    @Override
    public void write(byte[] packet, int len) {
        if(outputStream != null){
            try {
                outputStream.write(packet, 0,len);
            } catch (IOException e) {
                closeAccessory();
                try {sleep(1000);} catch (InterruptedException ignore) {}
                usbState = UsbState.DISCOVERING;
                connectionListener.OnConnectionStatus(false);
            }
        }else{
            Timber.v("No open stream to send %d bytes to USB", len);
        }
    }

}
