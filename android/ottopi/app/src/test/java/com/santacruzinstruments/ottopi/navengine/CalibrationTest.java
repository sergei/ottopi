package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.navengine.calibration.Calibrator;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

import junit.framework.TestCase;

public class CalibrationTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testAngleCalibration() {

        Calibrator calibrator = new Calibrator();

        CalibrationData cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertFalse(cal.isAwaValid);
        assertFalse(cal.isSpeedValid);

        calibrator.toggle(); // Start
        cal = calibrator.getCalibrationData();
        assertTrue(cal.isActive);

        // Use the example from B&G WIND manual mean port 35, mean starboard 25

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(-35))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(-34))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(-36))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(25))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(24))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(26))
                .build());

        calibrator.toggle(); // Stop

        cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertTrue(cal.isAwaValid);

        // Positive value: misalignment to port
        assertEquals(5, cal.awaBias, 0.1);

        // It was already compensated clockwise (to starboard by 1 degree)
        calibrator.setCurrentMisaligned(1);
        cal = calibrator.getCalibrationData();
        // Need to add five more
        assertEquals(6, cal.misalignmentValue, 0.1);
    }
    
    public final void testSpeedCalibration() {

        Calibrator calibrator = new Calibrator();

        CalibrationData cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertFalse(cal.isAwaValid);
        assertFalse(cal.isSpeedValid);

        calibrator.toggle(); // Start
        cal = calibrator.getCalibrationData();
        assertTrue(cal.isActive);

        // Use the example from B&G SPEED manual, display over reading by 10 %

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .sow(new Speed(6.54))
                .sog(new Speed(7.25))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .sow(new Speed(6.54))
                .sog(new Speed(7.25))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .sow(new Speed(6.54))
                .sog(new Speed(7.25))
                .build());
        
        calibrator.toggle(); // Stop

        cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertTrue(cal.isSpeedValid);

        // SOW 10% lower
        assertEquals(0.9, cal.sowRatio, 0.01);

        // Let's sau it set on factory 6.25 Hz / kt
        calibrator.setCurrentLogCal(6.25);
        cal = calibrator.getCalibrationData();

        // Decrease current LOG CAL by 10%, so the display will read higher
        assertEquals(5.63, cal.logCalValue, 0.1);
    }
    public final void testSpeedCalibrationN2K() {

        Calibrator calibrator = new Calibrator();

        CalibrationData cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertFalse(cal.isAwaValid);
        assertFalse(cal.isSpeedValid);

        assertFalse(cal.isSowCalValid);
        assertFalse(cal.isAwaCalValid);

        calibrator.toggle(); // Start
        cal = calibrator.getCalibrationData();
        assertTrue(cal.isActive);

        // Let's say
        // SOW reads 6.3 kts
        // current calibration is -10% ( so uncalibrated value is 7.0 kts)
        // The SOG is 7.7 kts, so the proper calibration should be +10%
        //

        calibrator.setCurrentSowCalPerc(-10);
        double uncalSow = Calibrator.getUncalSow(6.3, -10.);
        assertEquals(7, uncalSow, 0.01);

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .sow(new Speed(6.3))
                .sog(new Speed(7.7))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .sow(new Speed(6.3))
                .sog(new Speed(7.7))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .sow(new Speed(6.3))
                .sog(new Speed(7.7))
                .build());

        calibrator.toggle(); // Stop

        cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);

        assertTrue(cal.isSowCalValid);
        // SOW 10% lower
        assertEquals(10, cal.sowCalPerc, 0.01);
    }
    public final void testAngleCalibrationN2K() {

        Calibrator calibrator = new Calibrator();

        CalibrationData cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertFalse(cal.isAwaValid);
        assertFalse(cal.isSpeedValid);

        calibrator.toggle(); // Start
        cal = calibrator.getCalibrationData();
        assertTrue(cal.isActive);


        // It is supposed to read 30 degrees on both tacks
        // Uncalibrated values are 33 on port and 27 on starboard
        // Current calibration is -2 degrees
        // It reads mean port 35, mean starboard 25

        double uncalAwa = Calibrator.getUncalAwa(-35, -2);
        assertEquals(-33, uncalAwa, 0.1);

        uncalAwa = Calibrator.getUncalAwa(25, -2);
        assertEquals(27, uncalAwa, 0.1);

        calibrator.setCurrentAwaCalDeg(-2);

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(-35))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(-34))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(-36))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(25))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(24))
                .build());

        calibrator.onInstrumentInput(new InstrumentInput.Builder()
                .awa(new Angle(26))
                .build());

        calibrator.toggle(); // Stop

        cal = calibrator.getCalibrationData();
        assertFalse(cal.isActive);
        assertTrue(cal.isAwaCalValid);

        // Positive value: misalignment to port
        assertEquals(3, cal.awaCalDeg, 0.1);
    }

}
