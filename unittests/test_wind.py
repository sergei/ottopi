import datetime
import unittest

from gpxpy.geo import Location

from boat_model import BoatModel
from nav_window import NavWindow


class TestWind(unittest.TestCase):

    def test_short_window(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)
        nav_window = NavWindow()

        # Make sure state doesn't change until we have full window
        for instr_data in boat_model.intsr_data(start_utc, start_loc, NavWindow.WIN_LEN-1):
            state = nav_window.update(instr_data)
            self.assertEqual(state, NavWindow.STATE_UNKNOWN)

    def test_windward_rounding(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)
        boat_model = BoatModel(twd=0, tws=10, cog=-30, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the windward rounding
        boat_model.update(120, cog=-150)
        nav_window = NavWindow()

        # Analyze the four minutes of sailing
        duration = 240
        rounding_cnt = 0
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_ROUNDED_TOP:
                rounding_cnt += 1

        # There must be only one event of mark rounding
        self.assertEqual(1, rounding_cnt)

    def test_leeward_rounding(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the leeward rounding
        boat_model.update(120, cog=30)
        nav_window = NavWindow()

        # Analyze the four minutes of sailing
        duration = 240
        rounding_cnt = 0
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_ROUNDED_BOTTOM:
                rounding_cnt += 1

        # There must be only one event of mark rounding
        self.assertEqual(1, rounding_cnt)

    def test_tack(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)
        # Now do the tack
        boat_model.update(120, cog=-30)
        nav_window = NavWindow()

        # Analyze the four minutes of sailing
        duration = 240
        tack_cnt = 0
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_TACKED:
                tack_cnt += 1

        # There must be only one event of tacking
        self.assertEqual(1, tack_cnt)

    def test_gybe(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        boat_model = BoatModel(twd=0, tws=10, cog=150, sog=5, speed_rms=0.5, angle_rms=1)
        # Now gybe
        boat_model.update(120, cog=-150)
        nav_window = NavWindow()

        # Analyze the four minutes of sailing
        duration = 240
        gybe_cnt = 0
        for instr_data in boat_model.intsr_data(start_utc, start_loc, duration):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_TACKED:
                gybe_cnt += 1

        # There must be only one event of gybing
        self.assertEqual(1, gybe_cnt)

    def test_full_course(self):
        start_utc = datetime.datetime(year=2021, month=1, day=1, hour=0, minute=0, second=0)
        start_loc = Location(37., -122)

        # Sail diamond course, start on port
        t = 0
        boat_model = BoatModel(twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1)

        # Wind shift
        t += 60
        boat_model.update(t, twd=5, cog=35)
        # Now do the tack
        t += 60
        boat_model.update(t, cog=-25)
        # Wind shift
        t += 60
        boat_model.update(t, twd=0, cog=-30)
        # Now do the windward rounding
        t += 60
        boat_model.update(t, cog=-150)
        # Wind shift
        t += 60
        boat_model.update(t, twd=-5, cog=-155)
        # Gybe
        t += 60
        boat_model.update(t, cog=145)
        # Now do the leeward rounding
        t += 60
        boat_model.update(t, cog=25)
        t += 60

        tack_cnt = 0
        wm_cnt = 0
        lm_cnt = 0
        straight_cnt = 0

        nav_window = NavWindow()
        for instr_data in boat_model.intsr_data(start_utc, start_loc, t):
            state = nav_window.update(instr_data)
            if state == NavWindow.STATE_TACKED:
                tack_cnt += 1
            elif state == NavWindow.STATE_ROUNDED_TOP:
                wm_cnt += 1
            elif state == NavWindow.STATE_ROUNDED_BOTTOM:
                lm_cnt += 1
            elif state == NavWindow.STATE_STRAIGHT:
                straight_cnt += 1

        self.assertEqual(2, tack_cnt)
        self.assertEqual(1, wm_cnt)
        self.assertEqual(1, lm_cnt)

        self.assertGreater(straight_cnt, 0)


if __name__ == '__main__':
    unittest.main()
