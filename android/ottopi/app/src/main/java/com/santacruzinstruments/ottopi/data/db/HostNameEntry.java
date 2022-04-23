package com.santacruzinstruments.ottopi.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class HostNameEntry {

    @PrimaryKey
    @NonNull
    private final String name;
    private final long timestamp;

    public HostNameEntry(@NonNull String name, long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
