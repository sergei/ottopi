package com.santacruzinstruments.ottopi_start;

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
            i.putExtra("hot_button", "start_timer");
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        }

        Log.d("MainActivity", "Finishing");
        finish();
    }
}