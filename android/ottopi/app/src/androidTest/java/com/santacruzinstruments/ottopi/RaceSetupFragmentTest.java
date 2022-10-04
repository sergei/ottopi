package com.santacruzinstruments.ottopi;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.santacruzinstruments.ottopi.utils.ShowDropDownKt.showDropDown;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import android.view.Gravity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.GpxCollection;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
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

import java.io.File;
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

@LargeTest
@HiltAndroidTest
@UninstallModules(HiltModule.class)
@RunWith(AndroidJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RaceSetupFragmentTest {

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
                .perform(NavigationViewActions.navigateTo(R.id.raceSetupFragment));

        // Make sure we are in race setup fragments
        assertThat(navController.getCurrentDestination(),is(notNullValue()));
        assertThat(Objects.requireNonNull(navController.getCurrentDestination()).getId(),
                is(equalTo(R.id.raceSetupFragment)));
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
    public void showRaceTypes() {
        onView(ViewMatchers.withId(R.id.race_type_text)).perform(showDropDown());
        screenShotRecorder.captureScreen("Race type clicked" );

        onView(withText(R.string.start_at))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        screenShotRecorder.captureScreen("Start at selected" );

        onView(withId(R.id.startAtButton))
                .perform(click());
        screenShotRecorder.captureScreen("Start at clicked" );

        onView(withText("OK"))
                .perform(click());
        screenShotRecorder.captureScreen("Date set" );

        onView(withText("SET TIME"))
                .perform(click());
        screenShotRecorder.captureScreen("Time set" );
    }

    @Test
    public void showRaceRoute() {
        Route route = new Route();

        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("RCB")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.STARBOARD)
                .build());

        fakeNavManager.addRouteToRace(route);

        screenShotRecorder.captureScreen("RCB only" );

        route = new Route();

        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("PIN")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());

        fakeNavManager.addRouteToRace(route);

        screenShotRecorder.captureScreen("PIN only" );
        route = new Route();

        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("PIN")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());

        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("RCB")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.STARBOARD)
                .build());

        fakeNavManager.addRouteToRace(route);
        screenShotRecorder.captureScreen("Start line only" );

        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("W1")
                .type(RoutePoint.Type.ROUNDING)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());

        fakeNavManager.addRouteToRace(route);

        screenShotRecorder.captureScreen("Start line and top mark" );
        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("L1")
                .type(RoutePoint.Type.ROUNDING)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());

        fakeNavManager.addRouteToRace(route);
        screenShotRecorder.captureScreen("Start line and bottom mark" );

        screenShotRecorder.captureScreen("Start line and top mark" );

        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("Finish")
                .type(RoutePoint.Type.FINISH)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());


        fakeNavManager.addRouteToRace(route);

        screenShotRecorder.captureScreen(" Added finish pin" );
        route.addRpt(new RoutePoint.Builder()
                .loc((new GeoLoc(37, -122)))
                .name("RCB")
                .type(RoutePoint.Type.FINISH)
                .leaveTo(RoutePoint.LeaveTo.STARBOARD)
                .build());

        fakeNavManager.addRouteToRace(route);
        screenShotRecorder.captureScreen(" Added finish line" );
    }

    @Test
    public void manipulateRaceRoute() {
        Route route = new Route();

        long id = 0;
        route.addRpt(new RoutePoint.Builder()
                .id(++id)
                .loc((new GeoLoc(37, -122)))
                .name("PIN")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());

        route.addRpt(new RoutePoint.Builder()
                .id(++id)
                .loc((new GeoLoc(37, -122)))
                .name("RCB")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.STARBOARD)
                .build());
        route.addRpt(new RoutePoint.Builder()
                .id(++id)
                .loc((new GeoLoc(37, -122)))
                .name("W1")
                .type(RoutePoint.Type.ROUNDING)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());
        route.addRpt(new RoutePoint.Builder()
                .id(++id)
                .loc((new GeoLoc(37, -122)))
                .name("L1")
                .type(RoutePoint.Type.ROUNDING)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());
        route.addRpt(new RoutePoint.Builder()
                .id(++id)
                .loc((new GeoLoc(37, -122)))
                .name("Finish")
                .type(RoutePoint.Type.FINISH)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .build());

        screenShotRecorder.captureScreen(" Added finish pin" );
        route.addRpt(new RoutePoint.Builder()
                .id(++id)
                .loc((new GeoLoc(37, -122)))
                .name("RCB")
                .type(RoutePoint.Type.FINISH)
                .leaveTo(RoutePoint.LeaveTo.STARBOARD)
                .build());

        fakeNavManager.addRouteToRace(route);
        screenShotRecorder.captureScreen("Route created" );


        onView(withText("W1"))
                .perform(click());
        screenShotRecorder.captureScreen("W1 is active" );

        onView(withText("L1"))
                .perform(click());
        screenShotRecorder.captureScreen("L1 is active" );

        onView(withText("PIN - RCB"))
                .perform(click());
        screenShotRecorder.captureScreen("RCB is active" );

        onView(withText("Finish - RCB"))
                .perform(click());
        screenShotRecorder.captureScreen("Finish is active" );

        onView(allOf(withId(R.id.mtrl_list_item_icon),hasSibling(withText("PIN - RCB"))))
                .perform(click());

        screenShotRecorder.captureScreen("PIN point deleted" );

        onView(withText("W1"))
                .perform(click());
        screenShotRecorder.captureScreen("W1 is active" );

        onView(allOf(withId(R.id.mtrl_list_item_icon),hasSibling(withText("____ - RCB"))))
                .perform(click());

        screenShotRecorder.captureScreen("RCB point deleted" );

    }

    @Test
    public void createRoute() {
        LinkedList<File> files = new LinkedList<>();
        files.add(new File("/root/dir/Duxbury.gpx"));
        files.add(new File("/root/dir/SWIFT.gpx"));

        GpxCollection gpxCollection = new GpxCollection(files);
        fakeNavManager.onGpxCollectionUpdate(gpxCollection);

        onView(withId(R.id.gpx_name_text))
                .perform(showDropDown());

        onView(withText("Duxbury.gpx"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()));

        screenShotRecorder.captureScreen("GPX list clicked" );

        onView(withText("Duxbury.gpx"))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        screenShotRecorder.captureScreen("GPX file selected" );

        onView(withText("ocean"))
                .perform(click());

        screenShotRecorder.captureScreen("Ocean route selected" );

        onView(withText("SF"))
                .perform(click());

        screenShotRecorder.captureScreen("SF point added" );

        onView(withText("1DR"))
                .perform(click());

        screenShotRecorder.captureScreen("1DR point added" );

        onView(allOf(withText("1DR"),hasSibling(withId(R.id.mtrl_list_item_icon))))
                .perform(click());

        screenShotRecorder.captureScreen("1DR made active" );

        onView(allOf(withId(R.id.mtrl_list_item_icon),hasSibling(withText("SF"))))
                .perform(click());

        screenShotRecorder.captureScreen("SF point deleted" );

        onView(withText("ocean"))
                .perform(click());

        screenShotRecorder.captureScreen("Ocean collapsed" );

        onView(withText("bay"))
                .perform(click());

        screenShotRecorder.captureScreen("bay route selected" );

        onView(withText(R.string.add_entire_route))
                .perform(click());

        screenShotRecorder.captureScreen("bay route added" );
    }

    @Test
    public void nonFixedMarksRoute() {
        LinkedList<File> files = new LinkedList<>();
        files.add(new File("/root/dir/Duxbury.gpx"));
        files.add(new File("/root/dir/SWIFT.gpx"));

        GpxCollection gpxCollection = new GpxCollection(files);
        fakeNavManager.onGpxCollectionUpdate(gpxCollection);

        onView(withId(R.id.gpx_name_text))
                .perform(showDropDown());

        onView(withText("Duxbury.gpx"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()));

        screenShotRecorder.captureScreen("GPX list clicked" );

        onView(withText("Duxbury.gpx"))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        screenShotRecorder.captureScreen("GPX file selected" );

        onView(withText(R.string.inflatable_marks))
                .perform(click());

        screenShotRecorder.captureScreen("Non fixed marks clicked" );

        onView(withText(R.string.start_line))
                .perform(click());

        screenShotRecorder.captureScreen("Start Line" );

        onView(withText(R.string.windward_mark))
                .perform(click());

        screenShotRecorder.captureScreen("Inflatable mark" );

    }

}
