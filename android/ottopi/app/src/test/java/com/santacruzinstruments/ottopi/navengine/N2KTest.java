package com.santacruzinstruments.ottopi.navengine;

import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.SciWindCalibration_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.windData_pgn;
import static com.santacruzinstruments.N2KLib.Utils.Utils.radstodegs;
import static com.santacruzinstruments.ottopi.navengine.nmea2000.N2KCalibrator.makeGroupCommandPacket;
import static com.santacruzinstruments.ottopi.navengine.nmea2000.N2KCalibrator.makeGroupRequestPacket;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.santacruzinstruments.N2KLib.N2KLib.N2KField;
import com.santacruzinstruments.N2KLib.N2KLib.N2KLib;
import com.santacruzinstruments.N2KLib.N2KLib.N2KPacket;
import com.santacruzinstruments.N2KLib.N2KLib.N2KTypeException;
import com.santacruzinstruments.N2KLib.N2KMsgs.N2K;
import com.santacruzinstruments.ottopi.control.canbus.SerialUsbTransportTask;
import com.santacruzinstruments.ottopi.navengine.nmea2000.CanFrameAssembler;
import com.santacruzinstruments.ottopi.navengine.nmea2000.N2kListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class N2KTest {

    abstract static class TestN2kListener implements N2kListener {
        @Override
        public void OnConnectionStatus(boolean connected) {
        }

        @Override
        public void onTick() {
        }
    }

    private void parseLogSnippet(String logSnippet, CanFrameAssembler canFrameAssembler) {
        String [] lines = logSnippet.split("\n");
        for( String s : lines){
            int start = s.indexOf(",[");
            if ( start != -1 ){
                int end = s.indexOf("]");
                String msg = s.substring(start + 2, end);
                parseString(canFrameAssembler, msg);
            }
        }
    }

    @BeforeClass
    public static void SetupTimber(){
        Timber.plant(new Timber.Tree() {
            @Override
            protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
                System.out.println(message);
            }
        });
    }

    @Test
    public void decodingTest() throws IOException {
        InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("pgns.json");
        new N2KLib(null, is);
        AtomicBoolean twaDecoded = new AtomicBoolean(false);
        AtomicBoolean twsDecoded = new AtomicBoolean(false);
        CanFrameAssembler canFrameAssembler = new CanFrameAssembler();
        canFrameAssembler.addN2kListener(new N2kListener() {
            @Override
            public void onN2kPacket(N2KPacket packet)  {
                assertTrue(packet.isValid() || packet.pgn == 126998); // PGN 126998 not has supported field type of LENCTRLSTRING
                Timber.d("pkt %s\n", packet);

                if (packet.pgn == windData_pgn) {
                    try {
                        if (packet.fields[N2K.windData.windSpeed].getAvailability() == N2KField.Availability.AVAILABLE) {
                            double tws = packet.fields[N2K.windData.windSpeed].getDecimal();
                            twsDecoded.set(true);
                            System.out.printf("TWS=%.1f\n", tws);
                        }
                        if (packet.fields[N2K.windData.windAngle].getAvailability() == N2KField.Availability.AVAILABLE) {
                            double twa = radstodegs(packet.fields[N2K.windData.windAngle].getDecimal());
                            twaDecoded.set(true);
                            System.out.printf("TWA=%.1f\n", twa);
                        }
                    } catch (N2KTypeException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void OnConnectionStatus(boolean connected) {

            }

            @Override
            public void onTick() {

            }
        });

        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("raw.txt")));
        while (reader.ready()) {
            String s = reader.readLine();
            parseString(canFrameAssembler, s);
        }

        assertTrue(twaDecoded.get());
        assertFalse(twsDecoded.get());
    }

    private void parseString(CanFrameAssembler canFrameAssembler, String s) {
        String[] t = s.split(" ");
        if (t.length > 2) {
            if (Objects.equals(t[1], "R")) {
                int canAddr = Integer.parseInt(t[2], 16);
                byte[] data = new byte[t.length - 3];
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) Integer.parseInt(t[i + 3], 16);
                }
                canFrameAssembler.setFrame(canAddr, data);
            }
        }
    }

    @Test
    public void encodingTest() {

        final CanFrameAssembler canFrameAssembler = new CanFrameAssembler();

        // Need to create N2KLib() so some internal statics are initialized
        new N2KLib(null, Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("pgns.json"));

        N2KPacket p = makeGroupRequestPacket(SciWindCalibration_pgn);

        assertNotNull(p);

        List<byte[]> frames = canFrameAssembler.makeCanFrames(p);

        String [] expectedReq = {
                "09EDFF00 40 10 00 54 FF 01 FF FF\r\n",
                "09EDFF00 41 FF FF FF FF 02 01 E4\r\n",
                "09EDFF00 42 07 03 04 FF FF FF FF\r\n"
        };

        assertEquals(expectedReq.length, frames.size());

        List<String> generated = new LinkedList<>();
        for( byte[] data : frames){
            String msg = SerialUsbTransportTask.formatYdnuRawString(canFrameAssembler.getCanAddr(), data);
            generated.add(msg);
        }

        for( int i = 0; i < expectedReq.length; i++){
            assertEquals(expectedReq[i], generated.get(i));
        }

        p = makeGroupCommandPacket(SciWindCalibration_pgn, (byte)3, 4, 2.);
        String [] expectedCmd = {
                "09ED0300 40 0E 01 54 FF 01 FF 03\r\n",
                "09ED0300 41 01 E4 07 03 04 05 C8\r\n",
                "09ED0300 42 00 FF FF FF FF FF FF\r\n",
        };

        assertNotNull(p);

        frames = canFrameAssembler.makeCanFrames(p);
//        assertEquals(expectedCmd.length, frames.size());

        generated = new LinkedList<>();
        for( byte[] data : frames){
            String msg = SerialUsbTransportTask.formatYdnuRawString(canFrameAssembler.getCanAddr(), data);
            generated.add(msg);
        }

        for( int i = 0; i < expectedCmd.length; i++){
            assertEquals(expectedCmd[i], generated.get(i));
        }

    }

    @Test
    public void calDecodingTest(){
        Timber.d("Test");

        // Need to create N2KLib() so some internal statics are initialized
        CanFrameAssembler canFrameAssembler = makeCanFrameAssembler();
        canFrameAssembler.addN2kListener(new N2kListener() {
            @Override
            public void onN2kPacket(N2KPacket packet) {
                assertTrue(packet.isValid());
                Timber.d("pkt %s\n", packet);

                if (packet.pgn == SciWindCalibration_pgn) {
                    if ( packet.fields[N2K.SciWindCalibration.AWSMultiplier].getAvailability() == N2KField.Availability.AVAILABLE ){
                        try {
                            double awsCal = packet.fields[N2K.SciWindCalibration.AWSMultiplier].getDecimal();
                            assertEquals(10.81, awsCal, 0.001);
                        } catch (N2KTypeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void OnConnectionStatus(boolean connected) {

            }

            @Override
            public void onTick() {

            }
        });


        /*
I (323410) mhu2nmea_CalibrationStorage: Opening Non-Volatile Storage (NVS) handle...
I (323420) mhu2nmea_CalibrationStorage: Read AWA calibration 134
I (323420) mhu2nmea_CalibrationStorage: Opening Non-Volatile Storage (NVS) handle...
I (323430) mhu2nmea_CalibrationStorage: Read AWS calibration 1081
I (323440) mhu2nmea_ESP32N2kStream: 323111: Send PGN:130900323111:  - can ID:16772814320,6,9c,fc,86,0,39,4
I (323450) mhu2nmea_ESP32N2kStream: 323121 : Pri:2 PGN:130900 Source:15 Dest:255 Len:6 Data:9c,fc,86,0,39,4

         */
        String msg = "00:06:00.118 R 09FF540F C0 06 9C FC 86 00 39 04";
        parseString(canFrameAssembler, msg);


    }

    @Test
    public void invalidRepTestTest(){
        String logSnippet =
        "2023-04-10 08:46:43.074 SerialUsbTransportTask RAW_N2K,11,[15:46:44.850 R 09F1120F 7E 27 76 FF 7F FF 7F FD]\n" +
        "2023-04-10 08:46:43.080 SerialUsbTransportTask RAW_N2K,11,[15:46:44.850 R 0DF1190F 7E FF 7F 2E 35 50 3B FF]\n" +
        "2023-04-10 08:46:43.095 SerialUsbTransportTask RAW_N2K,11,[15:46:44.871 R 09F50310 71 FF FF FF FF 00 FF FF]\n" +
        "2023-04-10 08:46:43.175 SerialUsbTransportTask RAW_N2K,11,[15:46:44.950 R 09F1120F 8B 27 76 FF 7F FF 7F FD]\n" +
        "2023-04-10 08:46:43.196 SerialUsbTransportTask RAW_N2K,11,[15:46:44.972 R 09FD0211 72 FF FF 14 CB 02 FF FF]\n" +
        "2023-04-10 08:46:43.225 SerialUsbTransportTask RAW_N2K,11,[15:46:45.000 R 0DF0100F 93 F0 01 4C 50 C1 DB 21]\n" +
        "2023-04-10 08:46:43.232 SerialUsbTransportTask RAW_N2K,11,[15:46:45.001 R 09F8020F 93 FC C3 6B 84 06 FF FF]\n" +
        "2023-04-10 08:46:43.238 SerialUsbTransportTask RAW_N2K,11,[15:46:44.996 R 0DF8050F A0 2B 93 01 4C 50 C1 DB]\n" +
        "2023-04-10 08:46:43.245 SerialUsbTransportTask RAW_N2K,11,[15:46:44.997 R 0DF8050F A1 21 40 2D FC 03 14 51]\n" +
        "2023-04-10 08:46:43.250 SerialUsbTransportTask RAW_N2K,11,[15:46:44.997 R 0DF8050F A2 24 05 00 74 21 4D 07]\n" +
        "2023-04-10 08:46:43.256 SerialUsbTransportTask RAW_N2K,11,[15:46:44.998 R 0DF8050F A3 E2 13 EF 43 6A 8B 06]\n" +
        "2023-04-10 08:46:43.261 SerialUsbTransportTask RAW_N2K,11,[15:46:44.999 R 0DF8050F A4 00 00 00 00 10 FD 05]\n" +
        "2023-04-10 08:46:43.266 SerialUsbTransportTask RAW_N2K,11,[15:46:44.999 R 0DF8050F A5 77 00 00 00 00 00 00]\n" +
        "2023-04-10 08:46:43.271 SerialUsbTransportTask RAW_N2K,11,[15:46:45.000 R 0DF8050F A6 00 00 FF FF FF FF FF]\n" +
        "2023-04-10 08:46:43.275 Trace 01681141603274 !!! INVALID REP count 129029\n"
;

        Timber.d("Test");

        CanFrameAssembler canFrameAssembler = makeCanFrameAssembler();

        canFrameAssembler.addN2kListener(new TestN2kListener() {
            @Override
            public void onN2kPacket(N2KPacket packet) {
                assertTrue(packet.isValid());
                Timber.d("pkt %s\n", packet);

                if (packet.pgn == SciWindCalibration_pgn) {
                    if ( packet.fields[N2K.SciWindCalibration.AWSMultiplier].getAvailability() == N2KField.Availability.AVAILABLE ){
                        try {
                            double awsCal = packet.fields[N2K.SciWindCalibration.AWSMultiplier].getDecimal();
                            assertEquals(10.81, awsCal, 0.001);
                        } catch (N2KTypeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        parseLogSnippet(logSnippet, canFrameAssembler);

    }

    @NonNull
    private CanFrameAssembler makeCanFrameAssembler() {
        // Need to create N2KLib() so some internal statics are initialized
        new N2KLib(null, Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("pgns.json"));
        return new CanFrameAssembler();
    }

}
