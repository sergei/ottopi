package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

public class LowPassSpeedFilter {
    private double alpha;
    private double filteredValue;
    private UtcTime previousUtc = UtcTime.INVALID;
    private final double cutoffOmega;

    public LowPassSpeedFilter(double cutoffFrequency) {
        this.cutoffOmega = 2.0 * Math.PI * cutoffFrequency;
        this.alpha = 0.0;
        this.filteredValue = 0.0;
    }

    public Speed filter(UtcTime utc, Speed speed) {
        double input = speed.getKnots();

        if (previousUtc != UtcTime.INVALID) {
            double dt = (utc.toMiliSec()  - previousUtc.toMiliSec()) * 0.001;
            if ( dt > 5 )
                alpha = 1.0;
            else {
                alpha = dt / (1.0 / cutoffOmega + dt);
            }
        }

        filteredValue = alpha * input + (1 - alpha) * filteredValue;
        previousUtc = utc;
        return new Speed(filteredValue);
    }

}