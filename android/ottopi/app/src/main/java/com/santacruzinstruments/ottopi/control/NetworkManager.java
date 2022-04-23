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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class NetworkManager {

    public interface NetworkListener {
        void onUseWifi(boolean usWifi);
        void onConnectingToWifi(String ssid);
        void OnConnectingToHost(String url);
        void OnConnected(String url);
        void onDataReceived(byte[]  buff, int size);
        void onSsidScan(List<String> ssids);
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

    private long lastWifiReconnectAt = 0;

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

        InputStream nis = null;
        while ( bKeepRunning && useWifi) {
            nis = connectToTcpHost(hostname, port);
            if (nis == null)  // Failed to connect
                continue;

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
        }

        if (nis != null) {
            Timber.d("Closing the TCP connection");
            try {
                nis.close();
            } catch (IOException ignore) {}
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
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
            Timber.d("Trry to reconnect after %d sec", wifiConnectionAge);
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
