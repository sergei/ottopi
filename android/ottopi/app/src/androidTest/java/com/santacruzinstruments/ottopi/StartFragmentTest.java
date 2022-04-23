package com.santacruzinstruments.ottopi;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;

import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
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
public class StartFragmentTest {

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

    @Before
    public void before() {
        activityScenarioRule.getScenario().onActivity(activity -> screenShotRecorder = new ScreenShotRecorder(activity));

        // Make sure we are in the racing state to see the start fragment
        fakeNavManager.setSailingState(SailingState.PREPARATORY);
    }

    @After
    public void after(){
        screenShotRecorder.close();
    }


    @Test
    public void noData() {
        screenShotRecorder.captureScreen("No data" );
    }

    @Test
    public void favoredSide() {
        onView(ViewMatchers.withId(R.id.pinStartButton)).perform(click());
        screenShotRecorder.captureScreen("Pin button pressed" );

        onView(ViewMatchers.withId(R.id.committeeStartButton)).perform(click());
        screenShotRecorder.captureScreen("Rcb button pressed" );

        fakeNavManager.setDistToLine(new Distance(0.02), false);
        screenShotRecorder.captureScreen("Not OCS" );

        fakeNavManager.setDistToLine(new Distance(0.02), true);
        screenShotRecorder.captureScreen("OCS" );

        fakeNavManager.setPinFavoredBy(new Angle(30));
        screenShotRecorder.captureScreen("Port favored" );

        fakeNavManager.setPinFavoredBy(new Angle(-40));
        screenShotRecorder.captureScreen("Starboard favored" );

        onView(ViewMatchers.withId(R.id.timerButton)).perform(click());
        screenShotRecorder.captureScreen("Timer button pressed" );

    }

}
