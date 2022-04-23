package com.santacruzinstruments.ottopi.navengine.calibration;

import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.navengine.InstrumentInput;

import java.util.Collections;
import java.util.LinkedList;

public class Calibrator {
    private static final double MIN_SPEED = 3.;
    private final static int MAX_SIZE = 600;
    private final LinkedList<Double> deltaSpeeds = new LinkedList<>();
    private final LinkedList<Double> portAwas = new LinkedList<>();
    private final LinkedList<Double> stbdAwas = new LinkedList<>();
    private boolean isActive=false;

    private double currentLogCal = 6.25;
    private double currentMisaligned = 0;

    public void toggle(){
        if ( !isActive ){
            deltaSpeeds.clear();
            portAwas.clear();
            stbdAwas.clear();
            isActive = true;
        }else{
            isActive = false;
        }
    }

    public void onInstrumentInput(InstrumentInput ii) {
        if ( isActive ){
            if ( ii.sow.isValid() && ii.sog.isValid()
                    && ii.sow.getKnots() > MIN_SPEED && ii.sog.getKnots() > MIN_SPEED){
                double delta = ii.sow.getKnots() / ii.sog.getKnots();
                insertSorted(this.deltaSpeeds, delta);
            }
            if ( ii.awa.isValid()){
                double angle = ii.awa.toDegrees();
                if( angle < 0 ){
                    angle = - angle;
                    insertSorted(this.portAwas, angle);
                }else{
                    insertSorted(this.stbdAwas, angle);
                }
            }

            if ( this.deltaSpeeds.size() >= MAX_SIZE
                    || this.stbdAwas.size() >= MAX_SIZE
            || this.portAwas.size() >= MAX_SIZE)
                isActive = false;
        }
    }

    public CalibrationData getCalibrationData() {
        return new CalibrationData(
                isActive,
                isSpeedBiasValid(),
                isAwaBiaValid(),
                getPortBias(),
                getSowBiasRatio(),
                currentLogCal * getSowBiasRatio(),
                currentMisaligned + getPortBias()
        );
    }

    public double getCurrentLogCal() {
        return currentLogCal;
    }

    public double getCurrentMisaligned() {
        return currentMisaligned;
    }

    public void setCurrentLogCal(double currentLogCal) {
        this.currentLogCal = currentLogCal;
    }

    public void setCurrentMisaligned(double currentMisaligned) {
        this.currentMisaligned = currentMisaligned;
    }


    private boolean isSpeedBiasValid(){
        return !deltaSpeeds.isEmpty();
    }

    private boolean isAwaBiaValid(){
        return !portAwas.isEmpty() && !stbdAwas.isEmpty();
    }

    /// Greater than one if SOW is over reading
    private double getSowBiasRatio(){
        if ( isSpeedBiasValid() ){
            // Find median value
            return deltaSpeeds.get(deltaSpeeds.size() / 2);
        }else{
            return 1;
        }
    }

    // Positive means misaligned to port
    private double getPortBias() {
        if ( ! isAwaBiaValid() )
            return 0;

        double meanPortAwa = portAwas.get(portAwas.size() / 2);
        double meanStbdAwa = stbdAwas.get(stbdAwas.size() / 2);

        return (meanPortAwa - meanStbdAwa) / 2.;
    }

    private void insertSorted(LinkedList<Double> list, double value) {
        int index = Collections.binarySearch(list, value);
        if (index < 0) {
            index = -index - 1;
        }
        list.add(index, value);
    }

}
