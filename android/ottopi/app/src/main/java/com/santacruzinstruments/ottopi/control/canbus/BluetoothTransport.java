package com.santacruzinstruments.ottopi.control.canbus;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

public class BluetoothTransport implements CanBusWriter, SlipPacket.SlipListener, SlipPacket.SlipWriter {

    private  BluetoothSocket bluetoothSocket;
    private boolean isConnected = false;

    private static final String N2K_DEVICE_NAME = "N2kSerialPort";
    final UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private final Context context;
    private final N2KConnectionListener connectionListener;
    private boolean bKeepRunning = true;
    BluetoothAdapter btAdapter = null;
    SlipPacket slipPacket = new SlipPacket(this, this);


    public BluetoothTransport(Context context, N2KConnectionListener connectionListener) {
        this.context = context;
        this.connectionListener = connectionListener;
    }

    public void run() {
        Timber.d("Starting Bluetooth transport task thread");

        long lastTickMs = System.currentTimeMillis();
        while (bKeepRunning) {
            bluetoothSocket = findAndConnect();
            if ( bluetoothSocket != null){
                connectionListener.OnConnectionStatus(true);
                try {
                    InputStream inputStream = bluetoothSocket.getInputStream();
                    isConnected = true;
                    while (bKeepRunning && isConnected) {
                        int rcvdByte = inputStream.read();
                        if ( rcvdByte == -1){
                            Timber.d("Bluetooth connection lost");
                            break;
                        }
                        slipPacket.onSlipByteReceived(rcvdByte);
                        long now = System.currentTimeMillis();
                        if( now - lastTickMs > 1000){
                            lastTickMs = now;
                            connectionListener.onTick();
                        }
                    }
                    inputStream.close();
                } catch (IOException e) {
                    Timber.d("Bluetooth connection problem");
                }

                try {
                    bluetoothSocket.close();
                } catch (IOException ignore) {}
                connectionListener.OnConnectionStatus(false);
                isConnected = false;
            }

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connectionListener.onTick();
        }
    }

    private BluetoothSocket findAndConnect() {
        Timber.d("findAndConnect");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Timber.d("Bluetooth adapter is not available.");
            return null;
        }

        try {
            if (!btAdapter.isEnabled()) {
                Timber.d("Bluetooth adapter is not enabled.");
                return null;
            }
        } catch (SecurityException e) {
            Timber.e("Bluetooth security exception: %s", e.toString());
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("Bluetooth permission not granted");
                return null;
            }
        }

        BluetoothDevice btDevice = null;
        Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
        for (BluetoothDevice dev : bondedDevices) {
            Timber.d("Paired device: " + dev.getName() + " (" + dev.getAddress() + ")");

            if (dev.getName().equals(N2K_DEVICE_NAME)) {
                btDevice = dev;
            }
        }

        if (btDevice == null) {
            Timber.d("Target Bluetooth device is not found.");
            return null;
        }
        Timber.d("Target Bluetooth device is found.");

        BluetoothSocket btSocket;
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(sppUuid);
        } catch (IOException ex) {
            Timber.e("Failed to create RfComm socket: %s", ex.toString());
            return null;
        }
        Timber.d("Created a bluetooth socket.");

        for (int i = 0; ; i++) {
            try {
                btSocket.connect();
            } catch (IOException ex) {
                if (i < 5) {
                    Timber.i("Failed to connect. Retrying: %s", ex.toString());
                    continue;
                }

                Timber.e("Failed to connect: %s", ex.toString());
                return null;
            }
            break;
        }

        Timber.d("Connected to the device.");
        return btSocket;
    }

    public synchronized void stop(){
        bKeepRunning = false;
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
        Timber.d("BT_N2K,%d,[%s R %s]", len,
                TIME_FORMAT.format(new Date()),
                ydnuMsg.substring(0, ydnuMsg.length()-2));
        connectionListener.onFrameReceived(can_id, data);
    }

    private static final int BUFFER_SIZE = 16;

    @Override
    public void sendCanFrame(int canAddr, byte[] data) {
        if( bluetoothSocket != null){
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
    }

    @Override
    public void write(byte[] packet, int len) {
        if( isConnected ){
            try {
                bluetoothSocket.getOutputStream().write(packet, 0, len);
            } catch (IOException e) {
                Timber.e("Failed to write to bluetooth socket %s", e.toString());
                isConnected = false;
                connectionListener.OnConnectionStatus(false);
            }
        }
    }
}