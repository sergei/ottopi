package com.santacruzinstruments.ottopi.control;

import static android.net.wifi.SupplicantState.COMPLETED;
import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import androidx.annotation.UiThread;

import com.santacruzinstruments.ottopi.control.canbus.CanBusWriter;
import com.santacruzinstruments.ottopi.control.canbus.SerialUsbTransportTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class NetworkManager {

    private static final int UDP_TX_PORT = 2023;
    private static final int UDP_RX_PORT = 2024;

    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public interface NetworkListener {
        void onUseWifi(boolean usWifi);
        void onConnectingToWifi(String ssid);
        void OnConnectingToHost(String url);
        void OnConnected(String url);
        void onDataReceived(byte[]  buff, int size);
        void onN2kDataReceived(int can_id, byte[] data, int len);
        void onSsidScan(List<String> ssids);
    }

    private static class N2kSender implements CanBusWriter{
        InetSocketAddress gatewayAddress = null;
        void setGatewayAddress(InetAddress rcvdAddress){
            this.gatewayAddress = new InetSocketAddress(rcvdAddress, UDP_TX_PORT);
        }

        @Override
        public void sendCanFrame(int canAddr, byte[] data) {
            if( this.gatewayAddress == null)
                return;

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
                String ydnuMsg = SerialUsbTransportTask.formatYdnuRawString(canAddr, data);
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
    private static final long MIN_TIME_BETWEEN_WIFI_RECONNECTS_MS = 60 * 1000;
    private static final int BUFFER_SIZE = 4096;
    private static final int READ_TIMEOUT = 5000;

    private final WifiManager mWifiManager;
    private final NetworkListener networkListener;

    private boolean useWifi;
    private String hostname;
    private int port;
    private String ssid;
    private boolean bKeepRunning = true;
    private boolean bWaitingForWifi = true;
    private boolean bWaitingForTcp = true;

    private boolean bUseN2kUdp = true;

    private long lastWifiReconnectAt = 0;

    public CanBusWriter getCanBusWriter() {
        return n2kSender;
    }

    private final N2kSender n2kSender = new N2kSender();
    public NetworkManager(NetworkListener networkListener, boolean useWifi, String hostname, int port, String ssid, WifiManager mWifiManager) {
        this.networkListener = networkListener;
        this.hostname = hostname;
        this.port = port;
        this.ssid = ssid;
        this.mWifiManager = mWifiManager;
        this.useWifi = useWifi;
    }

    public void stop() {
        bKeepRunning = false;
        bWaitingForWifi = false;
        bWaitingForTcp = false;
    }

    public void run() {
        Timber.d("Starting network thread");

        bKeepRunning = true;

        postSsidList();

        while (bKeepRunning){
            networkListener.onUseWifi(useWifi);
            if ( useWifi ) {
                wifiLoop();
                // Exited from wifi loop because wifi was disabled by the user
            }else{
                noWifiLoop();
                // Exited from the loop because wifi was enabled by the user
            }
        }

        Timber.d("Network thread finished");
    }

    private void noWifiLoop() {
        Timber.d("Don't use WiFi");
        while (bKeepRunning && !useWifi){
            try {
                //noinspection BusyWait
                sleep(1000);
            } catch (InterruptedException ignore) {}
        }
    }

    private void wifiLoop() {

        Timber.d("Entering Wifi loop");
        WifiManager.WifiLock  wifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "OttoPiWiFiLock");
        wifiLock.setReferenceCounted(false);

        if (!wifiLock.isHeld())
            wifiLock.acquire();

        while ( bKeepRunning && useWifi) {
            if ( bUseN2kUdp){
                udpLoop();
            }else{
                tcpLoop();
            }
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void udpLoop() {

        connectToWiFi(this.ssid);

        // Create UDP socket bound to port 2023
        try {
            DatagramSocket socket = new DatagramSocket(UDP_RX_PORT);
            try {
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
//                    socket.bind(new InetSocketAddress(UDP_RX_PORT));
                socket.setSoTimeout(READ_TIMEOUT);
                byte[] buffer = new byte[BUFFER_SIZE];
                while (bKeepRunning && useWifi) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
                        socket.receive(packet);
                        final InetAddress gatewayAddress = packet.getAddress();
                        n2kSender.setGatewayAddress(gatewayAddress);

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
                        String ydnuMsg = SerialUsbTransportTask.formatYdnuRawString(can_id, data);
                        Timber.d("UDP_N2K,%d,[%s R %s]", len,
                                TIME_FORMAT.format(new Date()),
                                ydnuMsg.substring(0, ydnuMsg.length()-2));
                        networkListener.onN2kDataReceived(can_id, data, len);
                    } catch (IOException e) {
                        Timber.e("Network I/O error %s", e.getMessage());
                        break;
                    }
                }
            } catch (SocketException e) {
                Timber.e("Socket option error %s", e.getMessage());
            }
            socket.close();
        } catch (SocketException e) {
            Timber.e("Socket create error %s", e.getMessage());
        }

    }

    private void tcpLoop() {
        InputStream nis;
        nis = connectToTcpHost(hostname, port);
        if (nis == null)  // Failed to connect
            return;

        Timber.d("Start reading from %s:%d", hostname, port);

        byte[] buffer = new byte[BUFFER_SIZE];
        while (bKeepRunning && useWifi) {
            try {
                int read = nis.read(buffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    networkListener.onDataReceived(buffer, read);
                }
                if ( read == - 1){
                    Timber.d("Timeout reading from %s:%d", hostname, port);
                    //noinspection BusyWait
                    sleep(1000);
                    break;
                }
            } catch (IOException | InterruptedException e) {
                Timber.d("Network error %s", e.getMessage());
                break;
            }
        }

        Timber.d("Closing the TCP connection");
        try {
            nis.close();
        } catch (IOException ignore) {}
    }

    private InputStream connectToTcpHost(String hostname, int port) {

        bWaitingForTcp = true;
        while ( bWaitingForTcp ){
            connectToWiFi(this.ssid);
            try {
                final String url = String.format(Locale.getDefault(), "tcp://%s:%d", hostname, port);
                networkListener.OnConnectingToHost(url);
                Timber.d("Connecting to %s:%d", hostname, port);
                SocketAddress sockaddr = new InetSocketAddress(hostname, port);
                Socket nsocket = new Socket();
                int connectionTimeout = 5000;
                nsocket.connect(sockaddr, connectionTimeout);
                nsocket.setSoTimeout(READ_TIMEOUT);
                InputStream nis = nsocket.getInputStream();
                if (nsocket.isConnected()){
                    Timber.d("Connected to %s:%d", hostname, port);
                    networkListener.OnConnected(url);
                    return nis;
                }
            } catch (IOException e) {
                Timber.d("Failed to connect to %s:%d on %s network %s", hostname, port, ssid, e.getMessage());
                try {
                    //noinspection BusyWait
                    sleep(1000);
                } catch (InterruptedException ignore) {
                    break;
                }
            }
        }
        return null;
    }

    void connectToWiFi(String ssid){
        @SuppressLint("MissingPermission") List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();

        long wifiConnectionAge = SystemClock.elapsedRealtime() - lastWifiReconnectAt;

        if ( isConnectedToWifi(ssid) && wifiConnectionAge < MIN_TIME_BETWEEN_WIFI_RECONNECTS_MS){
            Timber.d("Already connected to %s, connection age %d", ssid, wifiConnectionAge);
            return;
        }else{
            Timber.d("Try to reconnect after %d sec", wifiConnectionAge);
        }

        List<String> ssids = new LinkedList<>();
        int networkId = -1;
        for( WifiConfiguration i : list ) {
            String s = i.SSID;
            if (s.startsWith("\""))
                s = s.substring(1);
            if (s.endsWith("\"") && s.length() > 1)
                s = s.substring(0, s.length() - 1);
            ssids.add(s);
            if ( s.equals(ssid)){
                networkId = i.networkId;
            }
        }
        networkListener.onSsidScan(ssids);

        if( ssids.contains(ssid)) {
            networkListener.onConnectingToWifi(ssid);
            Timber.d("Disconnecting from WiFi");
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(networkId, true);
            Timber.d("Connecting to %s", ssid);
            mWifiManager.reconnect();

            bWaitingForWifi = true;
            while ( bWaitingForWifi ){
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                SupplicantState supplicantState = wifiInfo.getSupplicantState();
                Timber.d("%s: SupplicantState %s", ssid, supplicantState);
                if ( supplicantState == COMPLETED ){
                    // Wait a second to have it fully initialized
                    try {
                        //noinspection BusyWait
                        sleep(1000);
                    } catch (InterruptedException ignore){}
                    lastWifiReconnectAt = SystemClock.elapsedRealtime();
                    break;
                }
                try {
                    //noinspection BusyWait
                    sleep(1000);
                } catch (InterruptedException ignore) {
                    break;
                }
            }
        }
    }

    private boolean isConnectedToWifi(String ssid) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        SupplicantState supplicantState = wifiInfo.getSupplicantState();
        Timber.d("%s: SupplicantState %s", ssid, supplicantState);
        if (supplicantState == COMPLETED) {
            return wifiInfo.getSSID().equals("\"" + ssid + "\"");
        }
        return false;
    }

    @UiThread
    public void setHostname(String hostname) {
        bWaitingForTcp = false;
        bWaitingForWifi = false;
        this.hostname = hostname;
    }

    @UiThread
    public void useN2kUdp(boolean useN2kUdp) {
        this.bUseN2kUdp = useN2kUdp;
    }

    @UiThread
    public void setPort(int port) {
        bWaitingForTcp = false;
        bWaitingForWifi = false;
        this.port = port;
    }

    @UiThread
    public void setSsid(String ssid) {
        bWaitingForWifi = false;
        this.ssid = ssid;
    }

    public void useWifi(boolean useWifi) {
        this.useWifi = useWifi;
    }

    void postSsidList(){
        @SuppressLint("MissingPermission") List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        List<String> ssids = new LinkedList<>();
        for( WifiConfiguration i : list ) {
            String s = i.SSID;
            if (s.startsWith("\""))
                s = s.substring(1);
            if (s.endsWith("\"") && s.length() > 1)
                s = s.substring(0, s.length() - 1);
            ssids.add(s);
        }
        networkListener.onSsidScan(ssids);
    }
}
