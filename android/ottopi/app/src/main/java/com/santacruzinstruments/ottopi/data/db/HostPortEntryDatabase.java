package com.santacruzinstruments.ottopi.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Database(entities = {HostPortEntry.class}, version = 2, exportSchema = false)
public abstract class HostPortEntryDatabase extends RoomDatabase {
    public abstract HostPortEntryDao hostPortEntryDao();

    private static volatile HostPortEntryDatabase INSTANCE;

    static HostPortEntryDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (HostPortEntryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    HostPortEntryDatabase.class,
                                    "host_ports"
                            )
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    Executors.newSingleThreadScheduledExecutor().execute(() -> {
                                        List<HostPortEntry> ports = new ArrayList<>();

                                        ports.add(new HostPortEntry(10110, 1));

                                        getDatabase(context.getApplicationContext())
                                                .hostPortEntryDao().insertAll(ports);
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
