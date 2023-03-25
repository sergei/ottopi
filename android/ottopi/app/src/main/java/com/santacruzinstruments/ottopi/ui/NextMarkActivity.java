package com.santacruzinstruments.ottopi.ui;

import static com.santacruzinstruments.ottopi.ui.OttopiActivity.INTENT_EXTRA_KEY;
import static com.santacruzinstruments.ottopi.ui.OttopiActivity.INTENT_EXTRA_NEXT_MARK;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import timber.log.Timber;

public class NextMarkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(NextMarkActivity.this, OttopiActivity.class);
        i.putExtra(INTENT_EXTRA_KEY, INTENT_EXTRA_NEXT_MARK);
        Timber.d("Sending intent %s", i);
        startActivity(i);

        Timber.d("Finishing timer activity");
        finish();
    }
}