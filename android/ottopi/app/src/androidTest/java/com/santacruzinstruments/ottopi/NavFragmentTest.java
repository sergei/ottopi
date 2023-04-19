package com.santacruzinstruments.ottopi;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.navengine.InstrumentInput;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Objects;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import dagger.hilt.components.SingletonComponent;
import timber.log.Timber;

@LargeTest
@HiltAndroidTest
@UninstallModules(HiltModule.class)
@RunWith(AndroidJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NavFragmentTest {

    private static final float MIN_SPEED_FACTOR = 0.3f;
    private static final float MAX_SPEED_FACTOR = 2.1f;
    private static final float SPEED_FACTOR_STEP = 0.5f;
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
        // Enable Timber logging
        Timber.plant(new Timber.DebugTree());
        activityScenarioRule.getScenario().onActivity(activity -> screenShotRecorder = new ScreenShotRecorder(activity));

        // Make sure we are in the racing state to see the nav fragment
        fakeNavManager.setSailingState(SailingState.RACING);
    }

    @After
    public void after(){
        screenShotRecorder.close();
    }

    static class History {
        final Speed sow;
        final Angle twa;
        final Speed tws;
        final Angle watm;
        final Angle medianPortTwa;
        final Angle portIqr;
        final Angle medianStbdTwa;
        final Angle stbdIqr;


        History(Speed sow, Angle twa, Speed tws) {
            this.sow = sow;
            this.twa = twa;
            this.tws = tws;
            this.watm = Angle.INVALID;
            this.medianPortTwa = Angle.INVALID;
            this.portIqr = Angle.INVALID;
            this.medianStbdTwa = Angle.INVALID;
            this.stbdIqr = Angle.INVALID;
        }

        History(Speed sow, Angle twa, Speed tws, Angle watm,
                Angle medianPortTwa, Angle portIqr, Angle medianStbdTwa, Angle stbdIqr) {
            this.sow = sow;
            this.twa = twa;
            this.tws = tws;
            this.watm = watm;
            this.medianPortTwa = medianPortTwa;
            this.portIqr = portIqr;
            this.medianStbdTwa = medianStbdTwa;
            this.stbdIqr = stbdIqr;
        }
    }

    @Test
    public void upwindStarboard() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(40), new Speed(12 * f)));
                add(new History(new Speed(7.1 * f), new Angle(35), new Speed(12 * f)));
                add(new History(new Speed(7.2 * f), new Angle(45), new Speed(12 * f)));
                add(new History(new Speed(7.3 * f), new Angle(60), new Speed(12 * f)));
            }};
            injectHistory(hist);
        }
    }

    @Test
    public void upwindPort() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(-40), new Speed(12 * f), new Angle(45),
                        new Angle(0), new Angle(20), new Angle(0), new Angle(20)));
                add(new History(new Speed(7.1 * f), new Angle(-35), new Speed(12 * f), new Angle(47),
                        new Angle(-30), new Angle(20), new Angle(30), new Angle(20)));
                add(new History(new Speed(7.2 * f), new Angle(-45), new Speed(12 * f), new Angle(48),
                        new Angle(-45), new Angle(20), new Angle(45), new Angle(20)));
                add(new History(new Speed(7.3 * f), new Angle(-60), new Speed(12 * f), new Angle(50),
                        new Angle(-90), new Angle(20), new Angle(90), new Angle(20)));
            }};
            injectHistory(hist);
        }
    }
    @Test
    public void downwindStarboard() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(140), new Speed(12 * f)));
                add(new History(new Speed(7.1 * f), new Angle(135), new Speed(12 * f)));
                add(new History(new Speed(7.2 * f), new Angle(145), new Speed(12 * f)));
                add(new History(new Speed(7.3 * f), new Angle(160), new Speed(12 * f)));
            }};
            injectHistory(hist);
        }
    }

    @Test
    public void downwindPort() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(-140), new Speed(12 * f)));
                add(new History(new Speed(7.1 * f), new Angle(-135), new Speed(12 * f)));
                add(new History(new Speed(7.2 * f), new Angle(-145), new Speed(12 * f)));
                add(new History(new Speed(7.3 * f), new Angle(-160), new Speed(12 * f)));
            }};
            injectHistory(hist);
        }
    }

    @Test
    public void reachStarboard() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(60), new Speed(12 * f)));
                add(new History(new Speed(7.1 * f), new Angle(80), new Speed(12 * f)));
                add(new History(new Speed(7.2 * f), new Angle(110), new Speed(12 * f)));
                add(new History(new Speed(7.3 * f), new Angle(120), new Speed(12 * f)));
            }};
            injectHistory(hist);
        }
    }

    @Test
    public void reachPort() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(-60), new Speed(12 * f)));
                add(new History(new Speed(7.1 * f), new Angle(-80), new Speed(12 * f)));
                add(new History(new Speed(7.2 * f), new Angle(-110), new Speed(12 * f)));
                add(new History(new Speed(7.3 * f), new Angle(-120), new Speed(12 * f)));
            }};
            injectHistory(hist);
        }
    }

    @Test
    public void allOver() throws IOException {

        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream("J105.txt");
        fakeNavManager.setPolarTable(new PolarTable(is));

        for(float speedFactor = MIN_SPEED_FACTOR; speedFactor <= MAX_SPEED_FACTOR; speedFactor += SPEED_FACTOR_STEP) {
            final float f = speedFactor;
            LinkedList<History> hist = new LinkedList<History>() {{
                add(new History(new Speed(7. * f), new Angle(60), new Speed(12 * f),
                        new Angle(50),
                        new Angle(0), new Angle(20),
                        new Angle(0), new Angle(20)));
                add(new History(new Speed(7.1 * f), new Angle(80), new Speed(12 * f),
                        new Angle(50),
                        new Angle(-30), new Angle(20),
                        new Angle(30), new Angle(20)));
                add(new History(new Speed(7.2 * f), new Angle(-110), new Speed(12 * f),
                        new Angle(50),
                        new Angle(-45), new Angle(20),
                        new Angle(45), new Angle(20)));
                add(new History(new Speed(7.3 * f), new Angle(-120), new Speed(12 * f),
                        new Angle(50),
                        new Angle(-90), new Angle(20),
                        new Angle(90), new Angle(20)));
                add(new History(new Speed(7.3 * f), new Angle(-60), new Speed(12 * f),
                        new Angle(50),
                        new Angle(-180), new Angle(20),
                        new Angle(180), new Angle(20)));
            }};
            injectHistory(hist);
        }
    }

    @Test
    public void noData() {
        screenShotRecorder.captureScreen("No data" );
    }

    @Test
    public void current() {

        for ( float speed = 0.1f; speed < 3.1f; speed += 0.5) {
            for ( float currentDir = 0; currentDir < 361.f; currentDir += 30.f){

                InstrumentInput ii = new InstrumentInput.Builder()
                        .cog(new Direction(0.))
                        .build();

                NavComputerOutput out = new NavComputerOutput.Builder(ii)
                        .sot( new Speed( speed))
                        .dot( new Direction(currentDir))
                        .destName("YRA-16")
                        .atm( new Angle(3))
                        .dtm( new Distance(10.5))
                        .nextDestName("Bonita")
                        .nextLegTwa(new Angle(120))
                        .build();

                fakeNavManager.setNavComputerOutput(out);
                screenShotRecorder.captureScreen(String.format("Current speed %.1f angle %.0f", speed, currentDir));
            }
        }
    }

    @Test
    public void testRoute() {
        screenShotRecorder.captureScreen("No Route" );

        InstrumentInput ii = new InstrumentInput.Builder()
                .build();

        String name = "YRA-16";
        double angle = 45;
        NavComputerOutput out = new NavComputerOutput.Builder(ii)
                .destName(name)
                .atm( new Angle(angle))
                .dtm( new Distance(1.5))
                .build();

        fakeNavManager.setNavComputerOutput(out);
        screenShotRecorder.captureScreen(String.format("Current speed %s angle %.0f", name, angle));

        name = "YRA-16";
        angle = -45;
        out = new NavComputerOutput.Builder(ii)
                .destName(name)
                .atm( new Angle(angle))
                .dtm( new Distance(0.001))
                .build();

        fakeNavManager.setNavComputerOutput(out);
        screenShotRecorder.captureScreen(String.format("Current speed %s angle %.0f", name, angle));

        name = "YRA-16";
        angle = -45;
        String nextDestName = "RED ROCK";
        double nextLegTwa = 120;
        out = new NavComputerOutput.Builder(ii)
                .destName(name)
                .atm( new Angle(angle))
                .dtm( new Distance(123.45))
                .nextDestName(nextDestName)
                .nextLegTwa(new Angle(nextLegTwa))
                .build();

        fakeNavManager.setNavComputerOutput(out);
        screenShotRecorder.captureScreen(String.format("Current speed %s angle %.0f, next %s %.0f",
                name, angle, nextDestName, nextLegTwa));

        nextDestName = "RED ROCK";
        nextLegTwa = -120;
        out = new NavComputerOutput.Builder(ii)
                .destName(name)
                .atm( new Angle(angle))
                .dtm( new Distance(10.5))
                .nextDestName(nextDestName)
                .nextLegTwa(new Angle(nextLegTwa))
                .build();

        fakeNavManager.setNavComputerOutput(out);
        screenShotRecorder.captureScreen(String.format("Current speed %s angle %.0f, next %s %.0f",
                name, angle, nextDestName, nextLegTwa));

        current();
    }

    private void injectHistory(LinkedList<History> hist) {
        for(History h : hist){
            InstrumentInput ii = new InstrumentInput.Builder()
                    .sow(h.sow)
                    .build();

            NavComputerOutput out = new NavComputerOutput.Builder(ii)
                    .tws(h.tws)
                    .twa(h.twa)
                    .watm(h.watm)
                    .medianPortTwa(h.medianPortTwa)
                    .portTwaIqr(h.portIqr)
                    .medianStbdTwa(h.medianStbdTwa)
                    .stbdTwaIqr(h.stbdIqr)
                    .build();

            fakeNavManager.setNavComputerOutput(out);
            screenShotRecorder.captureScreen(String.format("TWS:%s, TWA:%s, SOW:%s mSTBD:%s mPORT:%s",
                    h.tws, h.twa, h.sow, h.medianStbdTwa, h.medianPortTwa) );
        }
    }

}
