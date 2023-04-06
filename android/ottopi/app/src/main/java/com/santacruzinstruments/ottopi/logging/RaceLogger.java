package com.santacruzinstruments.ottopi.logging;

import android.annotation.SuppressLint;
import android.util.JsonWriter;

import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import timber.log.Timber;

@SuppressLint("SimpleDateFormat")
public class RaceLogger {
    static final SimpleDateFormat RACE_DIR_SDF = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    static final SimpleDateFormat ISO_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    private static final String NMEA_FILE_NAME = "race.nmea";
    private static final String JSON_FILE_NAME = "race.json";
    private final File rootDir;
    private UtcTime lastKnownUtc = UtcTime.INVALID;
    private UtcTime startTime = UtcTime.INVALID;
    private File raceDir;
    private File nmeaFile;
    private FileWriter nmeaWriter;
    private boolean isLogging = false;
    private Route route;
    private File jsonFile;
    private SailingState currentState = SailingState.CRUISING;

    public RaceLogger(File dir){
        this.rootDir = dir;
        //noinspection ResultOfMethodCallIgnored
        this.rootDir.mkdirs();
        ISO_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void onNmea(UtcTime utc, String nmea) {
        lastKnownUtc = utc;
        if( isLogging){
            try {
                nmeaWriter.write(nmea);
            } catch (IOException e) {
                isLogging = false;
                Timber.e("Failed to write NMEA file (%s)", e.getMessage());
            }
        }
    }

    public void onHeartBeat(SailingState state, long startTime){
        switch (currentState){
            case CRUISING:
                switch (state){
                    case PREPARATORY:
                        onPreparatory(startTime);
                        break;
                    case RACING:
                        onPreparatory(startTime);
                        onGun();
                        break;
                }
                break;
            case PREPARATORY:
                switch (state){
                    case CRUISING:
                        onFinish();
                        break;
                    case RACING:
                        onGun();
                        break;
                }
                break;
            case RACING:
                switch (state){
                    case CRUISING:
                        onFinish();
                        break;
                    case PREPARATORY:
                        break;
                }
                break;
        }
        currentState = state;
    }

    private void onPreparatory(long startTime){
        if ( this.rootDir.isDirectory() ){
            this.raceDir = new File (this.rootDir,  RACE_DIR_SDF.format(new Date(startTime)));
            isLogging = raceDir.mkdirs();
        }
        if ( isLogging ){
            nmeaFile = new File(this.raceDir, NMEA_FILE_NAME);
            try {
                nmeaWriter = new FileWriter( nmeaFile );
            } catch (IOException e) {
                isLogging = false;
                Timber.e("Failed to open NMEA file (%s)", e.getMessage());
            }
        }
    }

    private void onGun(){
        if ( lastKnownUtc.isValid()  )
            this.startTime = lastKnownUtc;
        else
            this.startTime = new UtcTime(new Date());
        writeJson();
    }

    public void onRouteUpdate(Route route){
        this.route = route;
        writeJson();
    }

    private void writeJson() {
        try {
            jsonFile = new File(raceDir, JSON_FILE_NAME);
            JsonWriter writer = new JsonWriter(new FileWriter(jsonFile));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("gun_at").value(ISO_TIME_FORMAT.format(this.startTime.getDate()));

            final int rptsNum = route.getRptsNum();
            if( rptsNum == 0 ){
                writer.name("marks").nullValue();
            }else{
                writer.name("marks");
                writer.beginArray();
                for(int i = 0; i < rptsNum; i++){
                    RoutePoint rpt = route.getRpt(i);
                    writer.beginObject();
                    writer.name("name").value(rpt.name);
                    writer.name("type").value(rpt.type.toString());
                    writer.name("leave_to").value(rpt.leaveTo.toString());
                    if( rpt.loc.isValid()){
                        writer.name("loc");
                        writer.beginArray();
                        writer.value(rpt.loc.lat);
                        writer.value(rpt.loc.lon);
                        writer.endArray();
                    }else{
                        writer.name("loc").nullValue();
                    }
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endObject();
            writer.close();
        } catch (IOException e) {
            Timber.e("Failed to write JSON file (%s)", e.getMessage());
        } catch (IllegalStateException e){
            Timber.e(e, "Failed to format JSON");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onFinish(){

        if ( isLogging ){
            isLogging = false;

            try {
                nmeaWriter.close();
            } catch (IOException e) {
                Timber.e("Failed to close NMEA file (%s)", e.getMessage());
            }

            if ( lastKnownUtc.toMiliSec() - startTime.toMiliSec() < 5 * 60 * 1000){
                Timber.d("Race was too short, deleting it");
                nmeaFile.delete();
                jsonFile.delete();
                raceDir.delete();
            }

        }
    }

}
