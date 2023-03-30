package com.santacruzinstruments.ottopi_stop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager manager = getPackageManager();
        Intent i = manager.getLaunchIntentForPackage("com.santacruzinstruments.ottopi");
        if (i != null) {
            Log.d("MainActivity", "Sending stop_race");
            i.putExtra("hot_button", "stop_race");
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        }

        Log.d("MainActivity", "Finishing");
        finish();
    }
}