package com.santacruzinstruments.ottopi.init;

import android.content.Context;

import java.io.File;

public class PathsConfig {

    private static File logsDir;
    private static File gpxdDir;
    private static File polarDir;
    private static File raceDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init(Context context) {
        logsDir = new File(context.getExternalCacheDir(), "logs");
        logsDir.mkdirs();
        gpxdDir = new File(context.getExternalCacheDir(), "gpx");
        gpxdDir.mkdirs();
        polarDir = new File(context.getExternalCacheDir(), "polars");
        polarDir.mkdirs();
        raceDir = new File(context.getExternalCacheDir(), "races");
        raceDir.mkdirs();
    }
    public static File getLogsDir() {
        return logsDir;
    }
    public static File getGpxDir() {
        return gpxdDir;
    }
    public static File getPolarDir() {
        return polarDir;
    }
    public static File getRaceDir() {
        return raceDir;
    }
}
