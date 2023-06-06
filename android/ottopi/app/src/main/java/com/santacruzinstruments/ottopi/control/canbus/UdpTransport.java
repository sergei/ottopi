package com.santacruzinstruments.ottopi.control.canbus;

import android.content.Context;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class UdpTransport implements CanBusWriter {

    private boolean isConnected = false;
    private static final int UDP_TX_PORT = 2023;
    private static final int UDP_RX_PORT = 2024;
    private static final int BUFFER_SIZE = 4096;
    private static final int READ_TIMEOUT = 1000;

    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private final Context context;
    private final N2KConnectionListener connectionListener;
    private boolean bKeepRunning = true;

    InetSocketAddress gatewayAddress = null;


    public UdpTransport(Context context, N2KConnectionListener connectionListener) {
        this.context = context;
        this.connectionListener = connectionListener;
    }

    private void udpLoop() {

        // Create UDP socket bound to port 2023
        try {
            DatagramSocket socket = new DatagramSocket(UDP_RX_PORT);
            try {
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
//                    socket.bind(new InetSocketAddress(UDP_RX_PORT));
                socket.setSoTimeout(READ_TIMEOUT);
                byte[] buffer = new byte[BUFFER_SIZE];
                while (bKeepRunning ) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
                        socket.receive(packet);
                        if( !isConnected ){
                            isConnected = true;
                            connectionListener.OnConnectionStatus(true);
                        }
                        setGatewayAddress(packet.getAddress());

                        int can_id;
                        byte[] data;
                        int len;
                        // can_id is first four bytes of packet
                        can_id = (packet.getData()[0] & 0xff) << 24 |
                                (packet.getData()[1] & 0xff) << 16 |
                                (packet.getData()[2] & 0xff) << 8 |
                                (packet.getData()[3] & 0xff);
                        // len is the fifth byte of packet
                        len = packet.getData()[4] & 0xff;
                        // data is the rest of the packet
                        data = new byte[len];
                        System.arraycopy(packet.getData(), 5, data, 0, len);
                        String ydnuMsg = SerialUsbTransport.formatYdnuRawString(can_id, data);
                        Timber.d("UDP_N2K,%d,[%s R %s]", len,
                                TIME_FORMAT.format(new Date()),
                                ydnuMsg.substring(0, ydnuMsg.length()-2));
                        connectionListener.onFrameReceived(can_id, data);
                    } catch (IOException e) {
                        if ( e instanceof SocketTimeoutException) {
                            connectionListener.onTick();
                        }else {
                            Timber.e("Socket receive error %s", e.getMessage());
                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                Timber.e("Socket option error %s", e.getMessage());
            }
            socket.close();
        } catch (SocketException e) {
            Timber.e("Socket create error %s", e.getMessage());
        }
        connectionListener.OnConnectionStatus(false);
        isConnected = false;
    }

    void setGatewayAddress(InetAddress rcvdAddress){
        this.gatewayAddress = new InetSocketAddress(rcvdAddress, UDP_TX_PORT);
    }


    public void run() {
        Timber.d("Starting UDP transport task thread");

        while (bKeepRunning) {
            udpLoop();
            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connectionListener.onTick();
        }
    }

    public synchronized void stop(){
        bKeepRunning = false;
    }

    @Override
    public void sendCanFrame(int canAddr, byte[] data) {
        if( this.gatewayAddress != null){
            try {
            DatagramSocket socket = new DatagramSocket();
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
            // Send the packet to port 2023
            DatagramPacket packet = new DatagramPacket(buffer, len, gatewayAddress.getAddress(), gatewayAddress.getPort());
            socket.send(packet);
            String ydnuMsg = SerialUsbTransport.formatYdnuRawString(canAddr, data);
            Timber.d("UDP_N2K,%d,[%s T %s]", len,
                    TIME_FORMAT.format(new Date()),
                    ydnuMsg.substring(0, ydnuMsg.length()-2));
            socket.close();
            } catch (SocketException e) {
                Timber.e("Error creating UDP socket %s ", e.getMessage());
            } catch (IOException e) {
                Timber.e("Error sending UDP packet %s", e.getMessage());
            }
        }
    }

}