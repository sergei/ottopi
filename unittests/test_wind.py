import datetime
import unittest
import random

from gpxpy.geo import Location

from boat_model import BoatModel
from nav_window import NavWindow, NavWndEventsListener


class TestWind(unittest.TestCase):

    def test_short_window(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)

        test_class = self

        # Make sure state doesn't change until we have full window
        class EventsListener(NavWndEventsListener):
            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.fail()  # Must not be here

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.fail()  # Must not be here

            def on_wind_shift(self, utc, loc, shift_deg, new_twd):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()

        nav_window = NavWindow(events_listener)
        for instr_data in boat_model.intsr_data(start_utc, start_loc, NavWindow.WIN_LEN-1):
            nav_window.update(instr_data)

    def test_windward_rounding(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        boat_model = BoatModel(twd=0, tws=10, cog=-30, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the windward rounding
        boat_model.update(120, cog=-150)
        test_class = self

        class EventsListener(NavWndEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.fail()  # Must not be here

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.assertTrue(is_windward)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_wind_shift(self, utc, loc, shift_deg, new_twd):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()

        nav_window = NavWindow(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_window.update(instr_data)

    def test_leeward_rounding(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the leeward rounding
        boat_model.update(120, cog=30)

        test_class = self

        class EventsListener(NavWndEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.fail()  # Must not be here

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.assertFalse(is_windward)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_wind_shift(self, utc, loc, shift_deg, new_twd):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()

        nav_window = NavWindow(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_window.update(instr_data)

    def test_tack(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the tack
        boat_model.update(120, cog=-30)
        test_class = self

        class EventsListener(NavWndEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.assertTrue(is_tack)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.fail()  # Must not be here

            def on_wind_shift(self, utc, loc, shift_deg, new_twd):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()

        nav_window = NavWindow(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_window.update(instr_data)

    def test_gybe(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # Now gybe
        boat_model.update(120, cog=-150)
        test_class = self

        class EventsListener(NavWndEventsListener):
            count = 0

            def on_tack(self, utc, loc, is_tack, distance_loss):
                test_class.assertFalse(is_tack)
                self.count += 1
                test_class.assertEqual(self.count, 1)

            def on_mark_rounding(self, utc, loc, is_windward):
                test_class.fail()  # Must not be here

            def on_wind_shift(self, utc, loc, shift_deg, new_twd):
                test_class.fail()  # Must not be here

            def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
                test_class.fail()  # Must not be here

        events_listener = EventsListener()

        nav_window = NavWindow(events_listener)

        # Analyze the four minutes of sailing
        duration = 240
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            nav_window.update(instr_data)

    def test_full_course(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        # Sail diamond course, start on port
        t = 0
        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)

        # Wind shift
        t += 120
        boat_model.update(t, twd=5, cog=35)
        # Now do the tack
        t += 120
        boat_model.update(t, cog=-25)
        # Wind shift
        t += 120
        boat_model.update(t, twd=0, cog=-30)
        # Now do the windward rounding
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
        boat_model.update(t, cog=25)
        t += 120

        tack_cnt = 0
        gybe_cnt = 0
        wm_cnt = 0
        lm_cnt = 0

        wind_shift_cnt = 0

        wind_shifts = [5, -5, -5]

        test_class = self

        class EventsListener(NavWndEventsListener):
            def on_tack(self, utc, loc, is_tack, distance_loss):
                nonlocal tack_cnt, gybe_cnt
                if is_tack:
                    tack_cnt += 1
                else:
                    gybe_cnt += 1

            def on_mark_rounding(self, utc, loc, is_windward):
                nonlocal wm_cnt, lm_cnt
                if is_windward:
                    wm_cnt += 1
                else:
                    lm_cnt += 1

            def on_wind_shift(self, utc, loc, shift_deg, new_twd):
                nonlocal wind_shift_cnt, wind_shifts
                test_class.assertAlmostEqual(shift_deg, wind_shifts[wind_shift_cnt], delta=1)
                wind_shift_cnt += 1

            def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
                pass

        events_listener = EventsListener()

        nav_window = NavWindow(events_listener)
        for instr_data in boat_model.intsr_data(start_utc, start_loc, t):
            nav_window.update(instr_data)

        self.assertEqual(1, tack_cnt)
        self.assertEqual(1, gybe_cnt)
        self.assertEqual(1, wm_cnt)
        self.assertEqual(1, lm_cnt)
        self.assertEqual(3, wind_shift_cnt)

    def test_compute_avg_twd(self):
        nav_window = NavWindow()

        # Fill with angles around th wrap point
        for i in range(1000):
            twd = random.gauss(0, 1) % 360
            nav_window.twd.append(twd)

        avg_twd = nav_window.compute_avg_twd()
        if avg_twd > 180:
            self.assertAlmostEqual(360., avg_twd, delta=1)
        else:
            self.assertAlmostEqual(0, avg_twd, delta=1)


if __name__ == '__main__':
    unittest.main()
