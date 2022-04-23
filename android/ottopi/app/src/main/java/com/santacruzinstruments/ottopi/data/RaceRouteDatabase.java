package com.santacruzinstruments.ottopi.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;


// To see the data base on a device using sqlite use
// adb shell
// # su
// # sqlite3  /data/data/com.santacruzinstruments.ottopi/databases/race_route
// .dump
@Database(entities = {RoutePoint.class}, version = 5)
@TypeConverters(Converters.class)
public abstract class RaceRouteDatabase  extends RoomDatabase {
    public abstract RaceRouteDao raceRouteDao();
}
