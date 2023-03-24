package com.santacruzinstruments.ottopi.data;

public class CalibrationData {
    final public boolean isActive;
    final public boolean isSpeedValid;
    final public double sowRatio;   // SOW / SOG  -  Greater than one if SOW is over reading
    final public boolean isAwaValid;
    final public double awaBias;   // AWAp - AWAs - Positive means misaligned to port
    // B&G NET suggested calibration values
    final public double logCalValue;
    final public double misalignmentValue;
    // SCI N2K suggested calibration values
    final public boolean isSowCalValid;
    final public double sowCalPerc;  // Suggested SOW calibration value, positive means the uncalibrated value is too low
    final public boolean isAwaCalValid;
    final public double awaCalDeg;  // Suggested AWA calibration value, positive means the uncalibrated value is too low

    public CalibrationData(boolean isActive, boolean isSpeedValid, boolean isAwaValid, double awaBias,
                           double sowRatio, double logCalValue, double misalignmentValue,
                           boolean isSowCalValid, double sowCalPerc, boolean isAwaCalValid, double awaCalDeg) {
        this.isActive = isActive;
        this.isSpeedValid = isSpeedValid;
        this.isAwaValid = isAwaValid;
        this.awaBias = awaBias;
        this.sowRatio = sowRatio;
        this.logCalValue = logCalValue;
        this.misalignmentValue = misalignmentValue;
        this.isSowCalValid = isSowCalValid;
        this.sowCalPerc = sowCalPerc;
        this.isAwaCalValid = isAwaCalValid;
        this.awaCalDeg = awaCalDeg;
    }
}
