package com.santacruzinstruments.ottopi.init;

import android.app.Application;

import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.logging.OttopiLogger;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class OttoPiApplication extends Application {

    private Thread.UncaughtExceptionHandler androidDefaultUEH;

    private final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
            Timber.wtf(ex);
            OttopiLogger.flush();
            androidDefaultUEH.uncaughtException(thread, ex);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Init file names
        PathsConfig.init(getApplicationContext());

        // Init logging
        OttopiLogger.init(getApplicationContext());
        Timber.d("First timber message");

        // Set uncaught exception handler
        androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

}
