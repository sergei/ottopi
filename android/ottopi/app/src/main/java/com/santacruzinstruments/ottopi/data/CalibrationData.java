package com.santacruzinstruments.ottopi.data;

public class CalibrationData {
    final public boolean isActive;
    final public boolean isSpeedValid;
    final public double sowRatio;   // SOW / SOG  -  Greater than one if SOW is over reading
    final public boolean isAwaValid;
    final public double awaBias;   // AWAp - AWAs - Positive means misaligned to port
    // SCI N2K suggested calibration values
    final public boolean isSowCalValid;
    final public double sowCalPerc;  // Suggested SOW calibration value, positive means the uncalibrated value is too low
    final public boolean isAwaCalValid;
    final public double awaCalDeg;  // Suggested AWA calibration value, positive means the uncalibrated value is too low

    final public int [] portAwaHist;
    final public int [] stbdAwaHist;
    final public int portAwaCount;
    final public int stbdAwaCount;

    final public int [] spdHist;
    final public int spdHistCount;

    final public float [] magDevDeg;

    public CalibrationData(boolean isActive, boolean isSpeedValid, boolean isAwaValid, double awaBias,
                           double sowRatio,
                           boolean isSowCalValid, double sowCalPerc, boolean isAwaCalValid, double awaCalDeg,
                           int [] portAwaHist, int [] stbdAwaHist, int portAwaCount, int stbdAwaCount, int[] spdHist, int spdHistCount, float[] magDevDeg) {
        this.isActive = isActive;
        this.isSpeedValid = isSpeedValid;
        this.isAwaValid = isAwaValid;
        this.awaBias = awaBias;
        this.sowRatio = sowRatio;
        this.isSowCalValid = isSowCalValid;
        this.sowCalPerc = sowCalPerc;
        this.isAwaCalValid = isAwaCalValid;
        this.awaCalDeg = awaCalDeg;
        this.portAwaHist = portAwaHist;
        this.stbdAwaHist = stbdAwaHist;
        this.portAwaCount = portAwaCount;
        this.stbdAwaCount = stbdAwaCount;
        this.spdHist = spdHist;
        this.spdHistCount = spdHistCount;
        this.magDevDeg = magDevDeg;
    }
}
