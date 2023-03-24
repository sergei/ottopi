package com.santacruzinstruments.ottopi.navengine.nmea2000;

import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.cogSogRapidUpdate_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.gnssPositionData_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.positionRapidUpdate_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.speed_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.systemTime_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.vesselHeading_pgn;
import static com.santacruzinstruments.N2KLib.N2KMsgs.N2K.windData_pgn;

import androidx.annotation.NonNull;

import com.santacruzinstruments.N2KLib.N2KLib.N2KField;
import com.santacruzinstruments.N2KLib.N2KLib.N2KPacket;
import com.santacruzinstruments.N2KLib.N2KLib.N2KTypeException;
import com.santacruzinstruments.N2KLib.N2KMsgs.N2K;
import com.santacruzinstruments.ottopi.navengine.InstrumentInput;
import com.santacruzinstruments.ottopi.navengine.InstrumentInputListener;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

public class InstrumentDataAssembler implements N2kListener{

    private static final GregorianCalendar mcal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

    private UtcTime utc = UtcTime.INVALID;
    private GeoLoc loc = GeoLoc.INVALID;
    private Direction cog = Direction.INVALID;
    private Speed sog = Speed.INVALID;
    private Speed aws = Speed.INVALID ;
    private Angle awa = Angle.INVALID;
    private Speed     tws = Speed.INVALID;
    private Angle     twa = Angle.INVALID;
    private Direction mag = Direction.INVALID;
    private Speed     sow = Speed.INVALID;

    LinkedList<InstrumentInputListener> mInstrumentInputListeners = new LinkedList<>();

    public void addInstrumentInputListener(InstrumentInputListener l){
        mInstrumentInputListeners.add( l );
    }

    @Override
    public void onN2kPacket(N2KPacket packet) throws N2KTypeException {
        boolean bGotLocation = false;
        switch(packet.pgn){
            case windData_pgn:
            {
                final boolean isApparent = packet.fields[N2K.windData.reference].getInt() == N2K.windData.reference_values.Apparent;
                final Speed windSpeed = getSpeed(packet, N2K.windData.windSpeed);
                final Angle windAngle = getAngle(packet, N2K.windData.windAngle);
                if( isApparent ){
                    this.aws = windSpeed;
                    this.awa = windAngle;
                }
                else{
                    this.tws = windSpeed;
                    this.twa = windAngle;
                }
            }
            break;
            case speed_pgn:
                this.sow = getSpeed(packet, N2K.speed.speedWaterReferenced);
            break;
            case vesselHeading_pgn:
            {
                final boolean isMagnetic = packet.fields[N2K.vesselHeading.reference].getInt() == N2K.vesselHeading.reference_values.Magnetic;
                if ( isMagnetic )
                    this.mag = getDirection(packet, N2K.vesselHeading.heading);
                else
                    this.mag = Direction.INVALID;
            }
            break;
            case systemTime_pgn:
                this.utc = getUtcTime(packet, N2K.systemTime.date, N2K.systemTime.time);
                break;
            case gnssPositionData_pgn:
            {
                this.utc = getUtcTime(packet, N2K.gnssPositionData.date, N2K.gnssPositionData.time);

                final int method = packet.fields[N2K.gnssPositionData.method].getInt();
                final boolean isValidMethod = method >= N2K.gnssPositionData.method_values.GNSSfix && method <= N2K.gnssPositionData.method_values.RTKfloat;

                if ( isValidMethod ){
                    this.loc = getGeoLoc(packet, N2K.gnssPositionData.latitude, N2K.gnssPositionData.longitude);
                }else{
                    this.loc = GeoLoc.INVALID;
                }
                bGotLocation = true;
            }
            break;
            case positionRapidUpdate_pgn:
                this.loc = getGeoLoc(packet, N2K.positionRapidUpdate.latitude, N2K.positionRapidUpdate.longitude);
                bGotLocation = true;
                break;
            case cogSogRapidUpdate_pgn:
            {
                final boolean isMagnetic = packet.fields[N2K.cogSogRapidUpdate.cogReference].getInt() == N2K.cogSogRapidUpdate.cogReference_values.Magnetic;
                if ( !isMagnetic){
                    this.cog = getDirection(packet, N2K.cogSogRapidUpdate.cogReference);
                }else{
                    cog = Direction.INVALID;
                }
                this.sog = getSpeed(packet, N2K.cogSogRapidUpdate.sog);
            }
            break;
        }

        if ( bGotLocation ){
            InstrumentInput ii = new InstrumentInput(this.utc, this.loc, this.cog, this.sog,
                    this.aws, this.awa, this.tws, this.twa, this.mag, this.sow);
            for( InstrumentInputListener l: mInstrumentInputListeners){
                l.onInstrumentInput(ii);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    @NonNull
    private Angle getAngle(N2KPacket packet, int angleField) throws N2KTypeException {
        final boolean hasAngle = packet.fields[angleField].getAvailability() == N2KField.Availability.AVAILABLE;
        Angle angle;
        if( hasAngle ){
            angle = new Angle(Math.toDegrees(packet.fields[angleField].getDecimal()));
        }else{
            angle = Angle.INVALID;
        }
        return angle;
    }

    private Direction getDirection(N2KPacket packet, int directionField) throws N2KTypeException {
        final boolean hasDirection = packet.fields[directionField].getAvailability() == N2KField.Availability.AVAILABLE;
        Direction direction;
        if( hasDirection ){
            direction = new Direction(Math.toDegrees(packet.fields[directionField].getDecimal()));
        }else{
            direction = Direction.INVALID;
        }
        return direction;
    }

    @NonNull
    private Speed getSpeed(N2KPacket packet, int speedField) throws N2KTypeException {
        Speed speed;
        final boolean hasSpeed = packet.fields[speedField].getAvailability() == N2KField.Availability.AVAILABLE;
        if ( hasSpeed ){
            speed = new Speed(Speed.Ms2Kts(packet.fields[speedField].getDecimal()));
        }else{
            speed = Speed.INVALID;
        }
        return speed;
    }

    @NonNull
    private GeoLoc getGeoLoc(N2KPacket packet, int latitudeField, int longitudeField) throws N2KTypeException {
        GeoLoc loc;
        boolean hasPosition = packet.fields[latitudeField].getAvailability() == N2KField.Availability.AVAILABLE
                && packet.fields[longitudeField].getAvailability() == N2KField.Availability.AVAILABLE;

        if( hasPosition ){
            double lat = packet.fields[latitudeField].getDecimal();
            double lon = packet.fields[longitudeField].getDecimal();
            loc = new GeoLoc(lat, lon);
        }else {
            loc = GeoLoc.INVALID;
        }
        return loc;
    }

    @NonNull
    private UtcTime getUtcTime(N2KPacket packet, int dateField, int timeField) throws N2KTypeException {
        UtcTime utc;
        boolean hasUtc = packet.fields[dateField].getAvailability() == N2KField.Availability.AVAILABLE
                && packet.fields[timeField].getAvailability() == N2KField.Availability.AVAILABLE;
        if ( hasUtc ){
            int days = packet.fields[dateField].getInt();
            double seconds = packet.fields[timeField].getDecimal();
            long milliSeconds = days * 24L * 3600L * 1000L + (long)(seconds * 1000);
            mcal.setTimeInMillis(milliSeconds);
            utc = new UtcTime(mcal.getTime());
        }else{
            utc = UtcTime.INVALID;
        }
        return utc;
    }
    @Override
    public void OnConnectionStatus(boolean connected) {

    }
    @Override
    public void onTick() {

    }
}
