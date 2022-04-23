package com.santacruzinstruments.ottopi.init;

import android.app.Application;

import com.santacruzinstruments.ottopi.logging.OttopiLogger;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class OttoPiApplication extends Application {

    private Thread.UncaughtExceptionHandler androidDefaultUEH;

    private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread thread, Throwable ex) {
            Timber.wtf(ex);
            OttopiLogger.flush();
            androidDefaultUEH.uncaughtException(thread, ex);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Init logging
        OttopiLogger.init(getApplicationContext());
        Timber.d("First timber message");

        // Set uncaught exception handler
        androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

}
