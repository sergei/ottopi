package com.santacruzinstruments.ottopi.navengine.geo;

import java.util.LinkedList;

public class DirectionSmoother {
    private double filteredNorth;
    private double filteredEast;
    private final double alpha;
    private final double turnThreshold;
    private final LinkedList<Direction> history = new LinkedList<>();

    public DirectionSmoother(double alpha, double turnThreshold){
        this.alpha = alpha;
        this.turnThreshold = turnThreshold;
    }

    public Direction update(Direction dir){
        boolean resetFilter = history.isEmpty();

        int histLen = 3;
        if( history.size() == histLen)
            history.removeFirst();
        history.add(dir);

        // Reset the filter if the turn is detected
        if( history.size() == histLen) {
            Angle a1 = Direction.angleBetween(history.get(0), history.get(1));
            Angle a2 = Direction.angleBetween(history.get(1), history.get(2));
            if ( a1.toDegrees() * a2.toDegrees() > 0 ){  // Anle have the same sign
                if( Math.abs(a1.toDegrees()) > turnThreshold &&  Math.abs(a2.toDegrees()) > turnThreshold){
                    // Turn detected
                    resetFilter = true;
                }
            }
        }

        // Compute east component and west component
        double north = Math.cos( dir.toRadians() );
        double east = Math.sin( dir.toRadians() );

        // To avoid the nonsense with rollover filter in cartesian coordinate system
        if ( resetFilter ){
            filteredNorth = north;
            filteredEast = east;
        }else{
            filteredNorth = filter(filteredNorth, north);
            filteredEast = filter(filteredEast, east);
        }

        return new Direction(Math.atan2(filteredEast, filteredNorth) * 180. / Math.PI);
    }

    private double filter( double f, double x )
    {
        return f * ( 1 - alpha) + x * alpha;
    }

}
