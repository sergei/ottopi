import datetime
import unittest

from gpxpy.geo import Location

from BoatModel import BoatModel
from nav_window import NavWindow


class TestWind(unittest.TestCase):

    def test_point_of_sail(self):

        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)
        nav_window = NavWindow()

        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        # Make sure state doesn't change until we have full window
        for instr_data in boat_model.intsr_data(start_utc, start_loc, NavWindow.WIN_LEN-1):
            state = nav_window.update(instr_data)
            self.assertEqual(state, NavWindow.STATE_UNKNOWN)

        # Now do the windward rounding
        boat_model = BoatModel(twd=0, tws=10, cog=-30, sog=5, speed_rms=0.5, angle_rms=1)
        # After two minutes bear away rounding
        boat_model.update(120, cog=-150)
        nav_window = NavWindow()

        # Analyze the four minutes of sailing
        duration = 240
        rounding_cnt = 0
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_ROUNDED_TOP:
                rounding_cnt += 1

        # There must be only one event of rounding top mark
        self.assertEqual(1, rounding_cnt)

        # Now do the leeward rounding
        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # After two minutes bear away rounding
        boat_model.update(120, cog=30)
        nav_window = NavWindow()

        # Analyze the four minutes of sailing
        duration = 240
        rounding_cnt = 0
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_ROUNDED_BOTTOM:
                rounding_cnt += 1

        # There must be only one event of rounding top mark
        self.assertEqual(1, rounding_cnt)


if __name__ == '__main__':
    unittest.main()
