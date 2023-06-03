package com.santacruzinstruments.ottopi.control.canbus;

public interface N2KConnectionListener {
    void OnConnectionStatus(boolean connected);

    void onFrameReceived(int addrPri, byte[] data);

    void onTick();
}
