package com.santacruzinstruments.ottopi.navengine.geo;

public class LowPassDirectionFilter {
    private final LowPassVectorFilter vectorFilter;
    private final Speed speed = new Speed(10);

    public LowPassDirectionFilter(double cutoffFrequency) {
        vectorFilter = new LowPassVectorFilter(cutoffFrequency);
    }

    public Direction filter(UtcTime utc, Direction dir) {

        if (dir == Direction.INVALID)
            return Direction.INVALID;

        vectorFilter.filter(utc, speed, new Angle(dir.toDegrees()));

        Angle filteredAngle = vectorFilter.getFilteredAngle();
        return new Direction( filteredAngle.toDegrees() );
    }


}
