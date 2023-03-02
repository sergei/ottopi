package com.santacruzinstruments.ottopi.navengine.nmea2000;

import com.santacruzinstruments.N2KLib.N2KLib.N2KPacket;
import com.santacruzinstruments.N2KLib.N2KLib.N2KTypeException;

public interface N2kListener {
    void onN2kPacket(N2KPacket packet) throws N2KTypeException;
}
