package com.santacruzinstruments.ottopi.navengine.geo;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

public class LowPassVectorFilter {
    private double alpha = 0.0;
    private double filteredXValue = 0.0;
    private double filteredYValue = 0.0;
    private UtcTime previousUtc = UtcTime.INVALID;
    private final double cutoffOmega;

    public LowPassVectorFilter(double cutoffFrequency) {
        this.cutoffOmega = 2.0 * Math.PI * cutoffFrequency;
    }

    public void filter(UtcTime utc, Speed speed, Angle angle) {

        if( speed == Speed.INVALID || angle == Angle.INVALID )
            return;

        double magnitude = speed.getKnots();
        double x = magnitude * Math.cos(angle.toRadians());
        double y = magnitude * Math.sin(angle.toRadians());

        if (previousUtc != UtcTime.INVALID) {
            double dt = (utc.toMiliSec()  - previousUtc.toMiliSec()) * 0.001;
            if ( dt > 5 )
                alpha = 1.0;
            else {
                alpha = dt / (1.0 / cutoffOmega + dt);
            }
        }

        filteredXValue = alpha * x + (1 - alpha) * filteredXValue;
        filteredYValue = alpha * y + (1 - alpha) * filteredYValue;
        previousUtc = utc;
    }

    public Speed getFilteredSpeed() {
        double magnitude = Math.sqrt(filteredXValue * filteredXValue + filteredYValue * filteredYValue);
        return new Speed(magnitude);
    }

    public Angle getFilteredAngle() {
        if (filteredXValue == 0.0 && filteredYValue == 0.0)
            return Angle.INVALID;
        return new Angle(Math.toDegrees(Math.atan2(filteredYValue, filteredXValue)));
    }

}