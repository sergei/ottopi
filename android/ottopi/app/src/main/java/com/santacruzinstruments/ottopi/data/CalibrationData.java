package com.santacruzinstruments.ottopi.data;

public class CalibrationData {
    final public boolean isActive;
    final public boolean isSpeedValid;
    final public double sowRatio;
    final public boolean isAwaValid;
    final public double awaBias;
    final public double logCalValue;
    final public double misalignmentValue;

    public CalibrationData(boolean isActive, boolean isSpeedValid, boolean isAwaValid, double awaBias, double sowRatio, double logCalValue, double misalignmentValue) {
        this.isActive = isActive;
        this.isSpeedValid = isSpeedValid;
        this.isAwaValid = isAwaValid;
        this.awaBias = awaBias;
        this.sowRatio = sowRatio;
        this.logCalValue = logCalValue;
        this.misalignmentValue = misalignmentValue;
    }
}
