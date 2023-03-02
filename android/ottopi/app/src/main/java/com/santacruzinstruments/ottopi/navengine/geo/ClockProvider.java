package com.santacruzinstruments.ottopi.navengine.geo;

import java.time.Clock;

public class ClockProvider {

    public static void setsClock(Clock clock) {
        sClock = clock;
    }

    public static Clock getClock() {
        return sClock;
    }

    private static Clock sClock = Clock.systemUTC();
}
