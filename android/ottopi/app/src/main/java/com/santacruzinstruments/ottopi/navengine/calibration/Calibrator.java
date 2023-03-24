package com.santacruzinstruments.ottopi.navengine.calibration;

import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.navengine.InstrumentInput;

import java.util.Collections;
import java.util.LinkedList;

public class Calibrator implements InstrCalibratorListener {
    private static final double MIN_SPEED = 3.;
    private final static int MAX_SIZE = 600*5;
    private final LinkedList<Double> deltaSpeeds = new LinkedList<>();
    private final LinkedList<Double> portAwas = new LinkedList<>();
    private final LinkedList<Double> stbdAwas = new LinkedList<>();
    private boolean isActive=false;

    private double currentLogCal = 6.25;
    private double currentMisaligned = 0;

    private boolean gotCurrentSowCal = false;
    private double currentSowCalPerc = 0;

    private boolean gotCurrentAwaCal = false;
    private double currentAwaCalDeg = 0;

    @Override
    public void setCurrentSowCalPerc(double currentSowCalPerc) {
        this.gotCurrentSowCal = true;
        this.currentSowCalPerc = currentSowCalPerc;
    }

    @Override
    public void setCurrentAwaCalDeg(double currentAwaCalDeg) {
        this.gotCurrentAwaCal = true;
        this.currentAwaCalDeg = currentAwaCalDeg;
    }

    public void toggle(){
        if ( !isActive ){
            deltaSpeeds.clear();
            portAwas.clear();
            stbdAwas.clear();
            isActive = true;
            gotCurrentAwaCal = false;
            gotCurrentSowCal = false;
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
                currentMisaligned + getPortBias(),
                isSowCalValid(), getSowCalPerc(),
                isAwaCalValid(), getAwaCalDeg()
        );
    }

    private double getAwaCalDeg() {
        if ( isAwaBiaValid() ){
            double portBias = getPortBias();
            double newPortBias = portBias - currentAwaCalDeg ;
            return newPortBias;
        }else{
            return 0;
        }
    }

    private boolean isAwaCalValid() {
        return isAwaBiaValid() && gotCurrentAwaCal;
    }

    private boolean isSowCalValid() {
        return isSpeedBiasValid() && gotCurrentSowCal;
    }

    private double getSowCalPerc() {
        if ( isSowCalValid() ){
            double sowVsSog = getSowBiasRatio();  // Cal vs calibrated value
            double currentRatio = 1 + currentSowCalPerc / 100.;
            double newRatio =  currentRatio / sowVsSog;
            double newCalPerc = (newRatio - 1) * 100;
            return newCalPerc;
        }else{
            return 0;
        }
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

        double medianPortAwa = portAwas.get(portAwas.size() / 2);
        double medianStbdAwa = stbdAwas.get(stbdAwas.size() / 2);

        return (medianPortAwa - medianStbdAwa) / 2.;
    }

    private void insertSorted(LinkedList<Double> list, double value) {
        int index = Collections.binarySearch(list, value);
        if (index < 0) {
            index = -index - 1;
        }
        list.add(index, value);
    }

    static public double getUncalAwa(double awa, double cal) {
        return awa - cal;
    }

    static public double getCalAwa(double reportedAwa, double currentCal, double suggestedCal) {
        return getUncalAwa(reportedAwa, currentCal) + suggestedCal;
    }

    public static double getUncalSow(double sow, double calPerc) {
        double calFactor = 1 + calPerc/100;
        return sow / calFactor;
    }

    static public double getCalSow(double reportedSow, double currentCal, double suggestedCal) {
        double calFactor = 1 + suggestedCal/100;
        return getUncalSow(reportedSow, currentCal) * calFactor;
    }

}
