package com.santacruzinstruments.ottopi.data;

import static com.santacruzinstruments.ottopi.navengine.route.RoutePoint.Type.ROUNDING;

import androidx.room.TypeConverter;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Converters {
    @TypeConverter
    public static GeoLoc fromGeoLoc( byte [] value) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(value);
            ObjectInput in = new ObjectInputStream(bis);
            GeoLoc o = (GeoLoc) in.readObject();
            in.close();
            return o;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @TypeConverter
    public static byte [] toGeoLoc(GeoLoc date) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(date);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert RoutePoint.Type to integer in order to e able to sort in the order defined in enum
    @TypeConverter
    public static int fromRoutePointType(RoutePoint.Type type){
        return type.ordinal();
    }

    @TypeConverter
    public static RoutePoint.Type fromRoutePointType(int ord){
        if ( ord < RoutePoint.Type.values().length )
            return RoutePoint.Type.values()[ord];
        else
            return ROUNDING;
    }


}
