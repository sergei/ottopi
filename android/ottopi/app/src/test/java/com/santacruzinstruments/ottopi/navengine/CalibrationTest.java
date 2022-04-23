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

}
