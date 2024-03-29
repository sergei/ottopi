package com.santacruzinstruments.ottopi.ui;


import static com.santacruzinstruments.ottopi.control.MainController.POLAR_FILE_NAME;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.santacruzinstruments.ottopi.BuildConfig;
import com.santacruzinstruments.ottopi.NavGraphDirections;
import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.databinding.ActivityOttopiBinding;
import com.santacruzinstruments.ottopi.init.PathsConfig;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
public class OttopiActivity extends AppCompatActivity {
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    public static final String INTENT_EXTRA_KEY = "hot_button";
    public static final String INTENT_EXTRA_STOP_RACE = "stop_race";
    public static final String INTENT_EXTRA_START_TIMER = "start_timer";

    private static final int UI_ANIMATION_DELAY = 300;
    private static final int FULL_SCREEN_INACTIVITY_TIMEOUT_MS = 5000;

    private static final int REQUEST_CODE_COARSE_LOCATION = 1;
    private static final int REQUEST_CODE_FINE_LOCATION = 2;
    private static final int REQUEST_CODE_BLUETOOTH_CONNECT = 3;

    private static final int REQUEST_CODE_BLUETOOTH = 4;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };
    //    private View mControlsView;
    private final Runnable mShowPart2Runnable = () -> {
        // Delayed display of UI elements
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = this::hide;
    private AppBarConfiguration appBarConfiguration;
    private NavViewModel navViewModel;
    private KeyMapper.CurrentScreen currentScreen = KeyMapper.CurrentScreen.OTHER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.d("Git branch: %s", BuildConfig.GIT_BRANCH);
        Timber.d("Git commit: %s", BuildConfig.GIT_HASH);

        requestPermissions();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        navViewModel = new ViewModelProvider(this).get(NavViewModel.class);

        ActivityOttopiBinding binding = ActivityOttopiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mVisible = true;
        mContentView = binding.navHostFragment;

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(view -> toggle());

        // Init nav controller
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        final NavController navController = Objects.requireNonNull(navHostFragment).getNavController();

        Set<Integer> topLevelDestinationIds = new HashSet<>();
        topLevelDestinationIds.add(R.id.startFragment);
        topLevelDestinationIds.add(R.id.navFragment);

        appBarConfiguration =
                new AppBarConfiguration.Builder(topLevelDestinationIds)
                        .setOpenableLayout(findViewById(R.id.main_coordinator))
                        .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        NavigationView navView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navView, navController);
        TextView giCommit = navView.getHeaderView(0).findViewById(R.id.gitCommit);
        giCommit.setText(getString(R.string.git_s, BuildConfig.GIT_HASH));
        TextView appVersion = navView.getHeaderView(0).findViewById(R.id.appVersion);
        appVersion.setText(getString(R.string.ver_s, BuildConfig.VERSION_NAME));

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Timber.d("Got Intent with action %s, type %s", action, type);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            String fileName = importFile(intent, type);
            if ( fileName != null){
                if( fileName.toLowerCase().endsWith(".gpx")){
                    NavGraphDirections.ActionGlobalRaceSetupFragment directions =
                            NavGraphDirections.actionGlobalRaceSetupFragment();
                    directions.setGpxName(fileName);
                    navController.navigate(directions);
                }else if( fileName.toLowerCase().endsWith(".pol")){
                    NavGraphDirections.ActionGlobalBoatSetupFragment directions =
                            NavGraphDirections.actionGlobalBoatSetupFragment();
                    directions.setPolarName(fileName);
                    navController.navigate(directions);
                }
            }
        }

        navViewModel.getCurrentScreen().observe(this, screen
                -> {
                        currentScreen =  screen;
                        if ( isFullScreenEnabled() ){
                            delayedHide(FULL_SCREEN_INACTIVITY_TIMEOUT_MS);
                        }else{
                            cancelDelayedHide();
                        }
                });

        if ( UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action) ){
            UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
            navViewModel.ctrl().setupUsbAccessory(accessory);
        }

        checkIntentForHotButtons(intent);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkIntentForHotButtons(intent);
    }

    private void checkIntentForHotButtons(Intent intent) {
        String cmd = intent.getStringExtra(INTENT_EXTRA_KEY);
        Timber.d("Got intent with cmd %s", cmd);
        if (Objects.equals(cmd, INTENT_EXTRA_START_TIMER)){
            navViewModel.ctrl().onStartButtonPress();
        }else if (Objects.equals(cmd, INTENT_EXTRA_STOP_RACE)){
            navViewModel.ctrl().onStopButtonPress();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        KeyMapper.Action action = KeyMapper.translateKeycode(currentScreen, keyCode);
        Timber.d("onKeyUp: Screen: %s Action: %s KeyEvent: %s", currentScreen, action, event);

        switch ( action ){
            case START_BUTTON:
                navViewModel.ctrl().onStartButtonPress();
                return true;
            case STOP_RACE:
                navViewModel.ctrl().onStopButtonPress();
                return true;
            case NEXT_MARK:
                navViewModel.ctrl().setNextMark();
                return true;
            case PREV_MARK:
                navViewModel.ctrl().setPrevMark();
                return true;
            case SET_PIN:
                navViewModel.ctrl().onPinButtonPress();
                return true;
            case SET_RCB:
                navViewModel.ctrl().onRcbButtonPress();
                return true;
            case NO_ACTION:
                return super.onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }

    private String importFile(Intent intent, String type) {
        Timber.d("Received type %s", type);
        Uri fileUri =  intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (fileUri != null) {
            try {
                ContentResolver contentResolver = getContentResolver();
                Cursor cursor = contentResolver.query(fileUri,null , null, null, null);
                cursor.moveToFirst();

                // Try to get the file name
                String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                if ( fileName == null ){
                    String pathName = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                    if ( pathName == null){
                        Timber.d("Failed to resolve the imported file name");
                        return null;
                    }
                    File f = new File(pathName);
                    fileName = f.getName();
                }

                int size = cursor.getInt(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
                cursor.close();
                Timber.d("Importing file %s, size %d", fileName, size);

                InputStream inputStream =  contentResolver.openInputStream(fileUri);

                byte [] buffer = new byte[size];
                for( int i=0; i < size; i++)
                    buffer[i] = (byte) inputStream.read();
                inputStream.close();

                final ByteArrayInputStream is = new ByteArrayInputStream(buffer);
                boolean isFileValid = false;
                File directory = null;
                if( fileName.toLowerCase().endsWith(".gpx")) {
                    // Verify if it's valid GPX file
                    RouteCollection rc = new RouteCollection(fileName);
                    rc.loadFromGpx(is);
                    // Since we did catch an exception let's store the GPX file
                    isFileValid = true;
                    directory = PathsConfig.getGpxDir();
                }else if( fileName.toLowerCase().endsWith(".pol")) {
                    // Verify if it's valid polar file
                    new PolarTable(is);
                    // Since we did catch an exception let's store the polar file
                    isFileValid = true;
                    fileName = POLAR_FILE_NAME; // Always have just one polar file with hardcoded name
                    directory = PathsConfig.getPolarDir();
                }

                if( isFileValid ) {
                    File f =new File(directory , fileName);
                    FileOutputStream fs = new FileOutputStream( f );
                    fs.write(buffer);
                    fs.close();
                    return fileName;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(1000);
    }

    private boolean isFullScreenEnabled(){
        return currentScreen == KeyMapper.CurrentScreen.NAV_SCREEN
                || currentScreen == KeyMapper.CurrentScreen.START_SCREEN;
    }

    private void toggle() {
        if (mVisible && isFullScreenEnabled()) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mContentView.getWindowInsetsController().show(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);

        // Hide after some inactivity
        if ( isFullScreenEnabled() ) {
            delayedHide(FULL_SCREEN_INACTIVITY_TIMEOUT_MS);
        }
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void cancelDelayedHide(){
        mHideHandler.removeCallbacks(mHideRunnable);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    private void requestPermissions() {
        // Request bluetooth connection permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_CODE_COARSE_LOCATION);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    REQUEST_CODE_BLUETOOTH);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.BLUETOOTH_CONNECT"},
                        REQUEST_CODE_BLUETOOTH_CONNECT);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_FINE_LOCATION) {
            if (grantResults.length > 0 // If request is cancelled, the result arrays are empty.
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(this);
                final boolean useInternalGps = sharedPreferences.getBoolean(getString(R.string.key_use_internal_gps), false);
                if (useInternalGps && isGpsEnabled()) {
                    navViewModel.ctrl().setUseInternalGps(true);
                }
            }
        }else if( requestCode == REQUEST_CODE_BLUETOOTH_CONNECT){
            if (grantResults.length > 0 // If request is cancelled, the result arrays are empty.
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.i("Bluetooth connect permission granted");
            }
        }else if( requestCode == REQUEST_CODE_BLUETOOTH){
            if (grantResults.length > 0 // If request is cancelled, the result arrays are empty.
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.i("Bluetooth permission granted");
            }
        }else if( requestCode == REQUEST_CODE_COARSE_LOCATION){
            if (grantResults.length > 0 // If request is cancelled, the result arrays are empty.
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.i("Coarse location granted");
            }
        }
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage("Please enable Location services amd then restart the app")
                    .setPositiveButton("Proceed to settings", (dialog, which)
                            -> enableLocationSettings())
                    .setNegativeButton("I don't want it", (dialog, which) -> {})
                    .show();
        }
        return gpsEnabled;
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }
}