package com.santacruzinstruments.ottopi.logging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;


import com.santacruzinstruments.ottopi.BuildConfig;
import com.santacruzinstruments.ottopi.R;

import java.io.File;

import timber.log.Timber;

public class OttopiLogger {

    private static final String TAG = "SimGpsLogger";
    private static FileLoggingTree mFileLoggingTree;

    public static void init(Context context){

        if (BuildConfig.DEBUG) {
            toggleLogging(context, true);
        } else {
            // Read settings
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            final boolean logsEnabled = sharedPreferences.getBoolean(context.getString(R.string.key_log_enabled), false);
            toggleLogging(context, logsEnabled);
            Timber.plant(new CrashReportingTree());
        }
    }

    /**
     * Toggle logging based on settings
     */
    @SuppressLint("LogNotTimber")
    public static void toggleLogging(Context context, boolean logsEnabled) {
        if (logsEnabled && mFileLoggingTree == null) {
            Log.d(TAG, "Enabling timber logging");
            mFileLoggingTree = new FileLoggingTree(context);
            Timber.plant(mFileLoggingTree);
        } else if (mFileLoggingTree != null) {
            Log.d(TAG, "Disabling timber logging");
            Timber.uproot(mFileLoggingTree);
            mFileLoggingTree = null;
        }
    }

    public static void flush(){
        if (mFileLoggingTree != null) {
            mFileLoggingTree.flush();
        }
    }

    public static File createUploadZip() {
        if (mFileLoggingTree != null) {
            return mFileLoggingTree.createUploadZip();
        }else{
            return null;
        }
    }

    public static void deleteUploadedFiles() {
        if (mFileLoggingTree != null) {
            mFileLoggingTree.deleteUploadedFiles();
        }
    }

    public static String getLogFileId() {
        if (mFileLoggingTree != null) {
            return  mFileLoggingTree.getLogFileId();
        }else{
            return "";
        }
    }

    /** A tree which logs important information for crash reporting. */
    private static class CrashReportingTree extends Timber.Tree {
        @Override protected void log(int priority, String tag, @NonNull String message, Throwable t) {
        }
    }
}
