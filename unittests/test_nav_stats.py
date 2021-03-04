import datetime
import io
import unittest
import random

from gpxpy.geo import Location

from boat_model import BoatModel
from nav_stats import NavStats, NavStatsEventsListener
from navigator import Targets
from polar_table import POLARS
from polars import Polars


class TestNavStats(unittest.TestCase):

    def test_short_window(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)

        test_class = self

        # Make sure state doesn't change until we have full window
        class EventsListener(NavStatsEventsListener):
            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.fail()  # Must not be here

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.fail()  # Must not be here

            def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()

        targets = Targets()
        nav_stats = NavStats(events_listener)
        for instr_data in boat_model.intsr_data(start_utc, start_loc, NavStats.WIN_LEN - 1):
            nav_stats.update(instr_data, targets)

    def test_windward_rounding(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        boat_model = BoatModel(twd=0, tws=10, cog=-30, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the windward rounding
        boat_model.update(120, cog=-150)
        test_class = self

        class EventsListener(NavStatsEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.fail()  # Must not be here

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.assertTrue(is_windward)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()
        targets = Targets()
        nav_stats = NavStats(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_stats.update(instr_data, targets)

    def test_leeward_rounding(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the leeward rounding
        boat_model.update(120, cog=30)

        test_class = self

        class EventsListener(NavStatsEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.fail()  # Must not be here

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.assertFalse(is_windward)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()
        targets = Targets()
        nav_stats = NavStats(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_stats.update(instr_data, targets)

    def test_tack(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the tack
        boat_model.update(120, cog=-30)
        test_class = self

        class EventsListener(NavStatsEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.assertTrue(is_tack)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.fail()  # Must not be here

            def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()
        targets = Targets()
        nav_stats = NavStats(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_stats.update(instr_data, targets)

    def test_gybe(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # Now gybe
        boat_model.update(120, cog=-150)
        test_class = self

        class EventsListener(NavStatsEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.assertFalse(is_tack)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.fail()  # Must not be here

            def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()
        targets = Targets()
        nav_stats = NavStats(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_stats.update(instr_data, targets)

    def test_targets(self):
        test_class = self
        mag = -14

        # Sail upwind slower and lower than targets on port
        boat_model = BoatModel(twd=0, tws=10, cog=45-mag, sog=5, speed_rms=0.5, angle_rms=1)

        class EventsListener(NavStatsEventsListener):
            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.assertGreater(twa_angle_delta, 0)  # > 0 means lower
                test_class.assertLess(speed_delta, 0)
                test_class.assertLess(distance_delta_m, 0)

        events_listener = EventsListener()
        nav_stats = NavStats(events_listener)
        self.sail_boat(boat_model, nav_stats)

        # Sail upwind slower and lower than targets on starboard
        boat_model = BoatModel(twd=0, tws=10, cog=-45-mag, sog=5, speed_rms=0.5, angle_rms=1)

        class EventsListener(NavStatsEventsListener):
            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.assertGreater(twa_angle_delta, 0)  # > 0 means lower
                test_class.assertLess(speed_delta, 0)
                test_class.assertLess(distance_delta_m, 0)

        events_listener = EventsListener()
        nav_stats = NavStats(events_listener)
        self.sail_boat(boat_model, nav_stats)

        # Sail upwind faster and higher than targets on port
        boat_model = BoatModel(twd=0, tws=10, cog=30-mag, sog=7, speed_rms=0.5, angle_rms=1)

        class EventsListener(NavStatsEventsListener):
            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.assertLess(twa_angle_delta, 0)  # < 0 means higher
                test_class.assertGreater(speed_delta, 0)
                test_class.assertGreater(distance_delta_m, 0)

        events_listener = EventsListener()
        nav_stats = NavStats(events_listener)
        self.sail_boat(boat_model, nav_stats)

        # Sail upwind faster and higher than targets on starboard
        boat_model = BoatModel(twd=0, tws=10, cog=-30-mag, sog=7, speed_rms=0.5, angle_rms=1)

        class EventsListener(NavStatsEventsListener):
            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.assertLess(twa_angle_delta, 0)  # < 0 means higher
                test_class.assertGreater(speed_delta, 0)
                test_class.assertGreater(distance_delta_m, 0)

        events_listener = EventsListener()
        nav_stats = NavStats(events_listener)
        self.sail_boat(boat_model, nav_stats)

        # Sail downwind slower and higher than targets on port
        boat_model = BoatModel(twd=0, tws=10, cog=130-mag, sog=5, speed_rms=0.5, angle_rms=1)

        class EventsListener(NavStatsEventsListener):
            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.assertLess(twa_angle_delta, 0)  # < 0 means higher
                test_class.assertLess(speed_delta, 0)
                test_class.assertLess(distance_delta_m, 0)

        events_listener = EventsListener()
        nav_stats = NavStats(events_listener)
        self.sail_boat(boat_model, nav_stats)

        # Sail downwind faster and lower than targets on port
        boat_model = BoatModel(twd=0, tws=10, cog=170-mag, sog=10, speed_rms=0.5, angle_rms=1)

        class EventsListener(NavStatsEventsListener):
            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                test_class.assertGreater(twa_angle_delta, 0)  # > 0 means lower
                test_class.assertGreater(speed_delta, 0)
                test_class.assertGreater(distance_delta_m, 0)

        events_listener = EventsListener()
        nav_stats = NavStats(events_listener)
        self.sail_boat(boat_model, nav_stats)

    def sail_boat(self, boat_model, nav_stats):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        polars = Polars()
        self.assertFalse(polars.is_valid())
        polar_file = io.StringIO(POLARS)
        polars.read_table(polar_file)
        self.assertTrue(polars.is_valid())
        for instr_data in boat_model.intsr_data(start_utc, start_loc, 120):
            targets = Targets(polars, instr_data.tws, instr_data.twa, instr_data.sow, instr_data.sog)
            nav_stats.update(instr_data, targets)

    def test_full_course(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        # Sail diamond course, start on port
        t = 0
        boat_model = BoatModel(twd=0, tws=10, cog=45, sog=5, speed_rms=0.5, angle_rms=1)
        # Wind shift
        t += 120
        boat_model.update(t, twd=5, cog=50)

        # Now do the tack
        t += 120
        boat_model.update(t, cog=-40, sog=3)
        # Sail slower for 10 seconds and go back to normal speed
        t += 10
        boat_model.update(t, sog=5)

        # Wind shift
        t += 110
        boat_model.update(t, twd=0, cog=-45)
        # Now do the windward rounding
        # Sail slower and lower
        t += 120
        boat_model.update(t, cog=-150)
        # Wind shift
        t += 120
        boat_model.update(t, twd=-5, cog=-155)
        # Gybe
        t += 120
        boat_model.update(t, cog=145)
        # Now do the leeward rounding
        t += 120
        boat_model.update(t, cog=50)
        t += 120

        tack_cnt = 0
        gybe_cnt = 0
        wm_cnt = 0
        lm_cnt = 0

        wind_shift_cnt = 0

        wind_shifts = [5, -5, -5]

        test_class = self

        class EventsListener(NavStatsEventsListener):
            def on_tack(self, utc, loc, is_tack, distance_loss):
                nonlocal tack_cnt, gybe_cnt
                if is_tack:
                    test_class.assertGreater(distance_loss, 1)
                    tack_cnt += 1
                else:
                    gybe_cnt += 1

            def on_mark_rounding(self, utc, loc, is_windward):
                nonlocal wm_cnt, lm_cnt
                if is_windward:
                    wm_cnt += 1
                else:
                    lm_cnt += 1

            def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
                nonlocal wind_shift_cnt, wind_shifts
                test_class.assertAlmostEqual(shift_deg, wind_shifts[wind_shift_cnt], delta=1)
                test_class.assertFalse(is_lift)
                wind_shift_cnt += 1

            def on_target_update(self, utc, loc, distance_delta_m, speed_delta, twa_angle_delta):
                pass

            def on_history_update(self, utc, loc_from, loc, avg_hdg, avg_twa):
                pass

        events_listener = EventsListener()

        polars = Polars()
        self.assertFalse(polars.is_valid())
        polar_file = io.StringIO(POLARS)
        polars.read_table(polar_file)
        self.assertTrue(polars.is_valid())

        nav_stats = NavStats(events_listener)
        for instr_data in boat_model.intsr_data(start_utc, start_loc, t):
            targets = Targets(polars, instr_data.tws, instr_data.twa, instr_data.sow, instr_data.sog)
            nav_stats.update(instr_data, targets)

        self.assertEqual(1, tack_cnt)
        self.assertEqual(1, gybe_cnt)
        self.assertEqual(1, wm_cnt)
        self.assertEqual(1, lm_cnt)
        self.assertEqual(3, wind_shift_cnt)

    def test_compute_avg_angle(self):

        # Fill with angles around the [359,0] wrap point
        angles = []
        for i in range(100):
            angle = random.gauss(0, 1) % 360
            angles.append(angle)

        avg_twd = NavStats.compute_avg_angle(angles, unsigned=True)
        if avg_twd > 180:
            self.assertAlmostEqual(360., avg_twd, delta=1)
        else:
            self.assertAlmostEqual(0, avg_twd, delta=1)

        # Fill with angles around the [-180,1800] wrap point
        angles = []
        for i in range(50):
            angle = random.gauss(-180, 1)
            angles.append(angle)
        for i in range(50):
            angle = random.gauss(180, 1)
            angles.append(angle)

        avg_twd = NavStats.compute_avg_angle(angles, unsigned=False)
        if avg_twd > 0:
            self.assertAlmostEqual(180., avg_twd, delta=1)
        else:
            self.assertAlmostEqual(-180, avg_twd, delta=1)


if __name__ == '__main__':
    unittest.main()
