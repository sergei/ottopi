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

@Database(entities = {HostNameEntry.class}, version = 1, exportSchema = false)
public abstract class HostNameEntryDatabase extends RoomDatabase {
    public abstract HostNameEntryDao hostNameEntryDao();

    private static volatile HostNameEntryDatabase INSTANCE;

    static HostNameEntryDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (HostNameEntryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    HostNameEntryDatabase.class,
                                    "host_names"
                            )
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    Executors.newSingleThreadScheduledExecutor().execute(() -> {
                                        List<HostNameEntry> hosts = new ArrayList<>();

                                        hosts.add(new HostNameEntry("localhost", 1));
                                        hosts.add(new HostNameEntry("192.168.43.100", 0));

                                        getDatabase(context.getApplicationContext())
                                                .hostNameEntryDao().insertAll(hosts);
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
