package com.santacruzinstruments.ottopi.navengine;

import androidx.core.util.Pair;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;

import java.util.Arrays;
import java.util.LinkedList;

public class WindStats {
    private final int QUEUE_LEN = 60;
    private final LinkedList<Double> portTwas = new LinkedList<>();
    private final LinkedList<Double> stbdTwas = new LinkedList<>();
    private final Double [] statsArray = new Double[QUEUE_LEN];

    /** Median true wind angle on port tack (either current or previous ) */
    private Angle medianPortTwa = Angle.INVALID;
    /** Interquartile range of true wind angle on port tack (either current or previous ) */
    private Angle portTwaIqr = Angle.INVALID;
    /** Median true wind angle on starboard tack (either current or previous ) */
    private Angle medianStbdTwa = Angle.INVALID;
    /** Interquartile range of true wind angle on starboard tack (either current or previous ) */
    private Angle stbdTwaIqr = Angle.INVALID;

    void update(Angle twa){
        if ( twa.isValid()) {

            double angle = twa.toDegrees();
            if( angle < 0 ){
                Pair<Double, Double> stats =  updateStats(angle, this.portTwas);
                if ( stats != null){
                    medianPortTwa = new Angle(stats.first);
                    portTwaIqr = new Angle(stats.second);
                }else{
                    medianPortTwa = Angle.INVALID;
                    portTwaIqr = Angle.INVALID;
                }
            }else{
                Pair<Double, Double> stats =  updateStats(angle, this.stbdTwas);
                if ( stats != null){
                    medianStbdTwa = new Angle(stats.first);
                    stbdTwaIqr = new Angle(stats.second);
                }else{
                    medianStbdTwa = Angle.INVALID;
                    stbdTwaIqr = Angle.INVALID;
                }
            }
        }
    }

    public Angle getMedianPortTwa(){
        return medianPortTwa;
    }

    public Angle getPortTwaIqr(){
        return portTwaIqr;
    }

    public Angle getMedianStbdTwa(){
        return medianStbdTwa;
    }

    public Angle getStbdTwaIqr(){
        return stbdTwaIqr;
    }

    private Pair<Double, Double> updateStats(double angle, LinkedList<Double> twas) {

        // Refresh data
        if ( twas.size() == QUEUE_LEN)
            twas.removeFirst();
        twas.addLast(angle);

        // Compute stats once have enough data
        if ( twas.size() == QUEUE_LEN ){
            twas.toArray(statsArray);
            Arrays.sort(statsArray);

            // Get quartiles
            double q1 = statsArray[QUEUE_LEN / 4];
            double q2 = statsArray[QUEUE_LEN / 2];
            double q3 = statsArray[QUEUE_LEN * 3 / 4];
            double iqr = q3 - q1;
            return new Pair<>(q2, iqr);
        }
        return null;
    }

}
