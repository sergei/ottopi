import io
import unittest
import datetime

import gpxpy
from gpxpy.gpx import GPXRoutePoint

from polar_table import POLARS
from polars import Polars
from navigator_listener import NavigationListener
from raw_instr_data import RawInstrData
from navigator import Navigator
from timer_talker import TimerTalker


class TestNav(unittest.TestCase):

    def test_dest_info(self):
        test_class = self

        class NavListener(NavigationListener):
            def on_dest_info(self, raw_instr_data, dest_info):
                test_class.assertAlmostEqual(dest_info.dtw, 0.92, delta=0.01)
                test_class.assertAlmostEqual(dest_info.btw, 228, delta=1)
                test_class.assertAlmostEqual(dest_info.atw, 14, delta=1)
                test_class.assertTrue(dest_info.atw_up)
                test_class.assertIsNone(dest_info.xte)
                test_class.assertIsNone(dest_info.bod)

        navigator = Navigator.get_instance()
        navigator.set_data_dir('/tmp/otto_test')
        listener = NavListener()
        navigator.add_listener(listener)

        lat = 37.871690
        lon = -122.359238
        instr_data = RawInstrData(t=0, utc=datetime.datetime.now(), lat=lat, lon=lon,
                                  sog=10, cog=200, awa=30, aws=15, twa=45, tws=10, sow=5, hdg=214)

        dest_wpt = GPXRoutePoint(name="DEST", latitude=37.864374, longitude=-122.376500)
        route = gpxpy.gpx.GPXRoute(name="RMB")
        route.points.append(dest_wpt)
        navigator.set_route(route, 0)
        navigator.set_raw_instr_data(instr_data)
        navigator.remove_listener(listener)

        class NavListener(NavigationListener):
            def on_dest_info(self, raw_instr_data, dest_info):
                test_class.assertAlmostEqual(dest_info.dtw, 0.92, delta=0.01)
                test_class.assertAlmostEqual(dest_info.btw, 228, delta=1)
                test_class.assertAlmostEqual(dest_info.atw, 14, delta=1)
                test_class.assertTrue(dest_info.atw_up)
                test_class.assertAlmostEqual(dest_info.xte, 0.455, delta=0.001)
                test_class.assertAlmostEqual(dest_info.bod, 200, delta=1)

        listener = NavListener()
        navigator.add_listener(listener)
        dest_wpt = GPXRoutePoint(name="DEST", latitude=37.864374, longitude=-122.376500)
        orig_wpt = GPXRoutePoint(name="ORIG", latitude=37.882646, longitude=-122.361774)
        route = gpxpy.gpx.GPXRoute(name="RMB")
        route.points.append(orig_wpt)
        route.points.append(dest_wpt)
        navigator.set_route(route, 1)
        navigator.set_raw_instr_data(instr_data)
        navigator.remove_listener(listener)

    def test_polars(self):
        polars = Polars()

        self.assertFalse(polars.is_valid())

        polar_file = io.StringIO(POLARS)
        polars.read_table(polar_file)

        # Upwind
        # Lowest wind in the table
        target_speed, target_twa = polars.get_targets(6, -30)
        self.assertAlmostEqual(target_twa, 44.6, delta=0.01)
        self.assertAlmostEqual(target_speed, 4.58, delta=0.01)

        # Exact wind in the table
        target_speed, target_twa = polars.get_targets(8, -30)
        self.assertAlmostEqual(target_twa, 41.7, delta=0.01)
        self.assertAlmostEqual(target_speed, 5.44, delta=0.01)

        # Highest wind in the table
        target_speed, target_twa = polars.get_targets(20, -30)
        self.assertAlmostEqual(target_twa, 39.5, delta=0.01)
        self.assertAlmostEqual(target_speed, 6.79, delta=0.01)

        # Extrapolate below lowest wind
        target_speed, target_twa = polars.get_targets(4, -30)
        self.assertAlmostEqual(target_twa, 47.5, delta=0.01)
        self.assertAlmostEqual(target_speed, 3.71, delta=0.01)

        # Extrapolate above strongest wind
        target_speed, target_twa = polars.get_targets(24, -30)
        self.assertAlmostEqual(target_twa, 40.3, delta=0.01)
        self.assertAlmostEqual(target_speed, 6.86, delta=0.01)

        # Interpolate within table wind range
        target_speed, target_twa = polars.get_targets(7, -30)
        self.assertAlmostEqual(target_twa, (44.6 + 41.7)/2, delta=0.01)
        self.assertAlmostEqual(target_speed, (5.44 + 4.58)/2, delta=0.01)

        # Downwind
        # Lowest wind in the table
        target_speed, target_twa = polars.get_targets(6, -130)
        self.assertAlmostEqual(target_twa, 140.5, delta=0.01)
        self.assertAlmostEqual(target_speed, 4.59, delta=0.01)

        # Exact wind in the table
        target_speed, target_twa = polars.get_targets(8, -130)
        self.assertAlmostEqual(target_twa, 144.8, delta=0.01)
        self.assertAlmostEqual(target_speed, 5.52, delta=0.01)

        # Highest wind in the table
        target_speed, target_twa = polars.get_targets(20, -130)
        self.assertAlmostEqual(target_twa, 178.7, delta=0.01)
        self.assertAlmostEqual(target_speed, 8.14, delta=0.01)

        # Self try to read invalid file

        polars.read_table('data/nav.gpx')

        # Make sure the polars are still valid
        self.assertTrue(polars.is_valid())
        target_speed, target_twa = polars.get_targets(20, -130)
        self.assertAlmostEqual(target_twa, 178.7, delta=0.01)
        self.assertAlmostEqual(target_speed, 8.14, delta=0.01)

    def test_count_down(self):
        time_talker = TimerTalker()
        time_talker.reset_time_checker(5 * 60.)
        for time_left_msec in range(5 * 60 * 10, 0, -1):
            time_left_sec = float(time_left_msec) / 10
            say_now, time_left = time_talker.check_time_left(time_left_sec)
            if say_now:
                self.assertAlmostEqual(time_left_msec/10, time_left, delta=0.2)

    def test_tws_twa(self):
        tws, twa = Navigator.compute_tws_twa(aws=10, awa=60, bs=5)
        self.assertAlmostEqual(tws, 8.7, delta=0.1)
        self.assertAlmostEqual(twa, 90, delta=0.5)

        tws, twa = Navigator.compute_tws_twa(aws=10, awa=-20, bs=5)
        self.assertAlmostEqual(tws, 5.6, delta=0.1)
        self.assertAlmostEqual(twa, -38, delta=0.5)


if __name__ == '__main__':
    unittest.main()
