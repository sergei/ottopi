package com.santacruzinstruments.ottopi;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import android.view.Gravity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.data.MeasuredDataType;
import com.santacruzinstruments.ottopi.init.HiltModule;
import com.santacruzinstruments.ottopi.ui.OttopiActivity;
import com.santacruzinstruments.ottopi.utils.FakeCtrlManager;
import com.santacruzinstruments.ottopi.utils.ScreenShotRecorder;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Objects;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import dagger.hilt.components.SingletonComponent;

@LargeTest
@HiltAndroidTest
@UninstallModules(HiltModule.class)
@RunWith(AndroidJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalibrationFragmentTest {

    private static FakeCtrlManager fakeNavManager;
    private ScreenShotRecorder screenShotRecorder;

    private final ActivityScenarioRule<OttopiActivity> activityScenarioRule =
            new ActivityScenarioRule<>(OttopiActivity.class);

    private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule
    public RuleChain rule = RuleChain
            .outerRule(hiltRule)
            .around(activityScenarioRule);

    @Module
    @InstallIn(SingletonComponent.class)
    public abstract static class TestModule {
        @Singleton
        @Provides
        public static CtrlInterface provideNavInterface(){
            fakeNavManager = new FakeCtrlManager();
            return fakeNavManager;
        }
    }

    private NavController navController;

    @Before
    public void before() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            screenShotRecorder = new ScreenShotRecorder(activity);
            navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
        });

        // Open Drawer to click on navigation.
        onView(withId(R.id.main_coordinator))
                .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        // Go to race setup
        onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.calibrationFragment));

        // Make sure we are in race setup fragments
        assertThat(navController.getCurrentDestination(),is(notNullValue()));
        assertThat(Objects.requireNonNull(navController.getCurrentDestination()).getId(),
                is(equalTo(R.id.calibrationFragment)));
    }

    @After
    public void after(){
        screenShotRecorder.close();
    }


    @Test
    public void notConnected() {
        screenShotRecorder.captureScreen("No data" );
    }

    @Test
    public void isConnected() {
        fakeNavManager.setN2KConnect(true);

        // Let's say
        // SOW reads 6.3 kts
        // current calibration is -10% ( so uncalibrated value is 7.0 kts)
        // The SOG is 7.7 kts, so the proper calibration should be +10%
        //
        CalibrationData calibrationData = new CalibrationData(false,
                true,
                true,
                5,       // AWAp - AWAs
                0.95,   // SOW / SOG
                0,
                0,
                true, -10,
                true, 8);
        fakeNavManager.setCalibrationData(calibrationData);
        fakeNavManager.setRcvdInstrCalibr(MeasuredDataType.SPD, -10);
        fakeNavManager.setRcvdInstrValue(MeasuredDataType.SPD, 6.3);
        screenShotRecorder.captureScreen("Is connected" );
        onView(withTagValue(is("calibrate"+"SPD")))
                .perform(click());
        screenShotRecorder.captureScreen("SPD CALIBRATE clicked" );
    }

}
