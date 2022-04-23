package com.santacruzinstruments.ottopi.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HostPortEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HostPortEntry> hostPorts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HostPortEntry hostPort);

    @Query("SELECT * FROM HostPortEntry ORDER BY timestamp DESC")
    LiveData<List<HostPortEntry>> getAllHostPorts();
}