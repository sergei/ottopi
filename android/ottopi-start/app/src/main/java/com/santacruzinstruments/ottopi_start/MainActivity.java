package com.santacruzinstruments.ottopi_start;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager manager = getPackageManager();
        final String pkg = "com.santacruzinstruments.ottopi";
        Intent i = manager.getLaunchIntentForPackage(pkg);
        if (i != null) {
            i.putExtra("hot_button", "start_timer");
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            Log.e(TAG, "Sending intent " + i + " to start timer");
            startActivity(i);
        }else{
            Log.e(TAG, "Could not find package " + pkg);
        }

        Log.d(TAG, "Finishing");
        finish();
    }
}