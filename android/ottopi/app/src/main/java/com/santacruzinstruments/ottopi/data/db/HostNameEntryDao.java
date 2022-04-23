package com.santacruzinstruments.ottopi.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HostNameEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HostNameEntry> hostNames);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HostNameEntry hostNames);

    @Query("SELECT * FROM HostNameEntry ORDER BY timestamp DESC")
    LiveData<List<HostNameEntry>> getAllHostnames();
}