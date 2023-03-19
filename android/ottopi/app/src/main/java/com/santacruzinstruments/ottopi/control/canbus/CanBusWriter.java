package com.santacruzinstruments.ottopi.control.canbus;

public interface CanBusWriter {
    void sendCanFrame(int canAddr, byte[] data);
}
