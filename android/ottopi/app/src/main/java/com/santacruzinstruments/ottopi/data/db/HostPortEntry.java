package com.santacruzinstruments.ottopi.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class HostPortEntry {

    @PrimaryKey
    private final int port;
    private final long timestamp;

    public HostPortEntry(int port, long timestamp) {
        this.port = port;
        this.timestamp = timestamp;
    }

    public int getPort() {
        return port;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
