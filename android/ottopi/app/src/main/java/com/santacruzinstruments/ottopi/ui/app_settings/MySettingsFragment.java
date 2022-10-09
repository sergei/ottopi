package com.santacruzinstruments.ottopi.ui.app_settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.logging.OttopiLogger;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.util.Objects;


@SuppressWarnings("unused")  // False warning, used in activity_settings.xml
public class MySettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private NavViewModel navViewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).
                registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).
                unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.key_log_enabled))) {
            final boolean logsEnabled = sharedPreferences.getBoolean(key, true);
            OttopiLogger.toggleLogging(logsEnabled);
        }
        if (key.equals(getString(R.string.key_use_internal_gps))) {
            final boolean useInternalGps = sharedPreferences.getBoolean(key, true);
            navViewModel.ctrl().setUseInternalGps(useInternalGps);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navViewModel = new ViewModelProvider(requireActivity()).get(NavViewModel.class);
    }
}
