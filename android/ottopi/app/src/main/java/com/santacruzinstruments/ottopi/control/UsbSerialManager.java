package com.santacruzinstruments.ottopi.control;

import static com.santacruzinstruments.ottopi.BuildConfig.APPLICATION_ID;

import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

import timber.log.Timber;

public class UsbSerialManager implements SerialInputOutputManager.Listener {

    private UsbDeviceConnection usbConnection;

    private enum Connected { False, Pending, True }
    private static final int WRITE_WAIT_MILLIS = 2000; // 0 blocked infinitely on unprogrammed arduino

    private static final String INTENT_ACTION_GRANT_USB = APPLICATION_ID + "INTENT_ACTION_GRANT_USB";
    private static final String INTENT_ACTION_DISCONNECT = APPLICATION_ID + "INTENT_ACTION_DISCONNECT";
    private boolean bKeepRunning = true;
    private final UsbReader.UsbConnectionListener usbConnectionListener;
    private final UsbManager usbManager;
    private UsbSerialPort usbSerialPort;
    private final PendingIntent usbPermissionIntent;
    private Connected connected = Connected.False;
    private final BroadcastReceiver disconnectBroadcastReceiver;
    private boolean gatewayInitialized = false;

    private SerialInputOutputManager ioManager;

    private UsbDevice usbDevice = null;
    boolean connectionEvent = false;

    private final Context context;

    public UsbSerialManager(Context context, UsbReader.UsbConnectionListener usbConnectionListener) {
        this.context = context;
        this.usbConnectionListener = usbConnectionListener;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        @SuppressLint("ObsoleteSdkInt") int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), flags);

        BroadcastReceiver permissionGrantedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("Received action %s", intent.getAction());
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
        context.registerReceiver(permissionGrantedBroadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

        disconnectBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                disconnect(); // disconnect now, else would be queued until UI re-attached
            }
        };
    }

    public synchronized void setUsbDevice(UsbDevice device) {
        this.usbDevice = device;
        connectionEvent = true;
    }

    public synchronized void stop(){
        bKeepRunning = false;
    }

    @SuppressWarnings("BusyWait")
    void run() {
        Timber.d("Starting Serial USB Manager thread");
        while (bKeepRunning) {

            if (connectionEvent){
                connectionEvent = false;
                connect(null);
            }

            if ( connected == Connected.True){
                if ( ! this.gatewayInitialized){
                    byte [] init = "YDNU MODE RAW\r\n".getBytes();
                    try {
                        write(init);
                        this.gatewayInitialized = true;
                    } catch (IOException e) {
                        Timber.e(e, "Could not write to YDNU");
                    }
                }
            }

            try {sleep(1000);} catch (InterruptedException ignore) {}
        }
    }

    private void connect(Boolean permissionGranted) {
        Timber.d("Connecting with %s permissions", permissionGranted == null ? "no" : "");
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(this.usbDevice);
        if(driver == null) {
            driver = CustomSerialUsbProber.getCustomProber().probeDevice(this.usbDevice);
        }
        if(driver == null) {
            Timber.e("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < 1) {
            Timber.e("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(0);
        usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            Timber.d("Requesting permission");
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }

        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                Timber.e("connection failed: permission denied");
            else
                Timber.e("connection failed: open failed");
            return;
        }

        connected = Connected.Pending;
        try {
            Timber.d("Opening port ...");
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            context.registerReceiver(disconnectBroadcastReceiver, new IntentFilter(INTENT_ACTION_DISCONNECT));
            usbSerialPort.setDTR(true); // for arduino, ...
            usbSerialPort.setRTS(true);
            ioManager = new SerialInputOutputManager(usbSerialPort, this);
            ioManager.start();
            usbConnectionListener.OnConnectionStatus(true);
            connected = Connected.True;
        } catch (Exception e) {
            Timber.e(e, "Failed to open port");
            usbConnectionListener.OnConnectionStatus(false);
        }

    }

    private void disconnect() {
        Timber.d("Starting to disconnect");
        if (ioManager != null) {
            ioManager.setListener(null);
            ioManager.stop();
            ioManager = null;
        }
        if (usbSerialPort != null) {
            try {
                usbSerialPort.setDTR(false);
                usbSerialPort.setRTS(false);
            } catch (Exception ignored) {
            }
            try {
                usbSerialPort.close();
            } catch (Exception ignored) {
            }
            usbSerialPort = null;
        }

        if(usbConnection != null) {
            usbConnection.close();
            usbConnection = null;
        }
        try {
            this.context.unregisterReceiver(disconnectBroadcastReceiver);
        } catch (Exception ignored) {
        }
        connected = Connected.False;
        usbConnectionListener.OnConnectionStatus(false);
    }

    void write(byte[] data) throws IOException {
        if(usbSerialPort == null)
            throw new IOException("not connected");
        usbSerialPort.write(data, WRITE_WAIT_MILLIS);
    }

    @Override
    public void onNewData(byte[] data) {
        Timber.d("Got %d bytes:", data.length);
        Timber.d("[%s]", new String(data));
    }

    @Override
    public void onRunError(Exception e) {
        Timber.w(e, "USB error");
        disconnect();
    }


}

