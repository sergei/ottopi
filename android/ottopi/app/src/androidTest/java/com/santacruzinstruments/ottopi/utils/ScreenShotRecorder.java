package com.santacruzinstruments.ottopi.utils;

import static android.graphics.Bitmap.CompressFormat.PNG;

import android.app.Activity;
import android.util.Log;

import androidx.test.runner.screenshot.CustomScreenCaptureProcessor;
import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.ScreenCaptureProcessor;
import androidx.test.runner.screenshot.Screenshot;


import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import timber.log.Timber;


public class ScreenShotRecorder {
    private static final int SLEEP_BEFORE_SHOT_MS = 1000;
    private ScreenCaptureProcessor screenCaptureProcessor;
    private Writer mdStream;

    public ScreenShotRecorder(Activity activity) {

        File screenshotDir = new File(activity.getExternalCacheDir(), "screenshots");
        boolean directoryExists = true;
        if ( !screenshotDir.isDirectory() )
            directoryExists = screenshotDir.mkdirs();

        Log.d("ScreenShotRecorder", String.format("Will store screenshots to %s", screenshotDir));
        if ( directoryExists ) {
            File markDown = new File(screenshotDir, "screenshots.json");
            try {
                mdStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(markDown, true)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            screenCaptureProcessor = new CustomScreenCaptureProcessor(screenshotDir);
        }
    }

    public void captureScreen(String title) {
        captureScreen(title, null);
    }

    private static class ScreenShot{
        final String header;
        final String image;
        final String body;

        private ScreenShot(String header, String image, String body) {
            this.header = header;
            this.image = image;
            this.body = body;
        }
    }

    public void captureScreen(String title, String body) {

        if ( screenCaptureProcessor != null ) {
            try { Thread.sleep(SLEEP_BEFORE_SHOT_MS); } catch (InterruptedException ignore) {}

            ScreenCapture cap = Screenshot.capture();
            cap.setFormat(PNG);

            try {
                String fileName = screenCaptureProcessor.process(cap);
                Gson gson = new Gson();
                ScreenShot sh = new ScreenShot(title, fileName, body);
                String s = gson.toJson(sh);
                mdStream.write(s);
                mdStream.write(",\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (mdStream != null) {
            try {
                mdStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
