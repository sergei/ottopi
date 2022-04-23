package com.santacruzinstruments.ottopi.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.util.List;

@Dao
public interface RaceRouteDao {
    @Insert
    void insertAll(RoutePoint... pts);

    @Insert
    long insert(RoutePoint pt);

    @Delete
    void delete(RoutePoint pt);

    @Query("DELETE FROM RoutePoint")
    void deleteAll();

    @Query("DELETE FROM RoutePoint where Type = :pointType")
    void deleteByType(RoutePoint.Type pointType);

    @Update
    void update(RoutePoint... pts);

    @Query("SELECT * FROM RoutePoint ORDER By Type")
    List<RoutePoint> getAll();

    @Query("SELECT * FROM RoutePoint WHERE isActive = 1")
    List<RoutePoint> getAllActive();

}
