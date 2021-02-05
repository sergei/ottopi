import unittest
import datetime

import gpxpy
from gpxpy.gpx import GPXRoutePoint

from dest_info import DestInfo
import nmea_encoder
from raw_instr_data import RawInstrData
from navigator import Navigator


class TestStringMethods(unittest.TestCase):

    def test_dest_info(self):
        test_class = self

        class NavListener:
            def on_dest_info(self, raw_instr_data, dest_info):
                test_class.assertAlmostEqual(dest_info.dtw, 0.92, delta=0.01)
                test_class.assertAlmostEqual(dest_info.btw, 228, delta=1)
                test_class.assertAlmostEqual(dest_info.atw, 14, delta=1)
                test_class.assertTrue(dest_info.atw_up)
                test_class.assertIsNone(dest_info.xte)
                test_class.assertIsNone(dest_info.bod)

        navigator = Navigator.get_instance()
        l = NavListener()
        navigator.add_listener(l)

        lat = 37.871690
        lon = -122.359238
        instr_data = RawInstrData(t=0, utc=datetime.datetime.now(), lat=lat, lon=lon,
                                  sog=10, cog=200, awa=30, aws=15, twa=45, tws=10, sow=5, hdg=214)

        dest_wpt = GPXRoutePoint(name="DEST", latitude=37.864374, longitude=-122.376500)
        route = gpxpy.gpx.GPXRoute(name="RMB")
        route.points.append(dest_wpt)
        navigator.set_route(route, 0)
        navigator.update(instr_data)
        navigator.remove_listener(l)

        class NavListener:
            def on_dest_info(self, raw_instr_data, dest_info):
                test_class.assertAlmostEqual(dest_info.dtw, 0.92, delta=0.01)
                test_class.assertAlmostEqual(dest_info.btw, 228, delta=1)
                test_class.assertAlmostEqual(dest_info.atw, 14, delta=1)
                test_class.assertTrue(dest_info.atw_up)
                test_class.assertAlmostEqual(dest_info.xte, 0.455, delta=0.001)
                test_class.assertAlmostEqual(dest_info.bod, 200, delta=1)

        l = NavListener()
        navigator.add_listener(l)
        dest_wpt = GPXRoutePoint(name="DEST", latitude=37.864374, longitude=-122.376500)
        orig_wpt = GPXRoutePoint(name="ORIG", latitude=37.882646, longitude=-122.361774)
        route = gpxpy.gpx.GPXRoute(name="RMB")
        route.points.append(orig_wpt)
        route.points.append(dest_wpt)
        navigator.set_route(route, 1)
        navigator.update(instr_data)
        navigator.remove_listener(l)

    def test_encode_bwr(self):
        utc = datetime.datetime(2020, 5, 17, 11, 45, 57, tzinfo=datetime.timezone.utc)
        instr_data = RawInstrData(t=0, utc=utc, lat=37.864374, lon=-122.376500,
                                  sog=10, cog=200, awa=30, aws=15, twa=45, tws=10, sow=5, hdg=214)
        dest_info = DestInfo()
        dest_info.wpt = GPXRoutePoint(name="DEST", latitude=37.864374, longitude=-122.376500)
        dest_info.xte = 0.455
        dest_info.dtw = 0.92
        dest_info.btw = 228
        nmea = nmea_encoder.encode_bwr(instr_data, dest_info)
        self.assertEqual(nmea, "$OPBWR,114557,3751.86244,N,12222.59000,W,,T,228.0,M,0.920,N,DEST,*0D\r\n")

    def test_encode_rmb(self):
        utc = datetime.datetime(2020, 5, 17, 11, 45, 57, tzinfo=datetime.timezone.utc)
        instr_data = RawInstrData(t=0, utc=utc, lat=37.864374, lon=-122.376500,
                                  sog=10, cog=200, awa=30, aws=15, twa=45, tws=10, sow=5, hdg=214)
        dest_info = DestInfo()
        dest_info.wpt = GPXRoutePoint(name="DEST", latitude=37.864374, longitude=-122.376500)
        dest_info.org_wpt = GPXRoutePoint(name="ORIG", latitude=37.864374, longitude=-122.376500)
        dest_info.xte = 0.455
        dest_info.dtw = 0.92
        dest_info.stw = 1.2345567
        dest_info.btw_true = 214
        dest_info.is_in_circle = True
        nmea = nmea_encoder.encode_rmb(dest_info)
        self.assertEqual(nmea, "$OPRMB,A,0.455,R,ORIG,DEST,3751.86244,N,12222.59000,W,0.920,214.0,1.2,A,*26\r\n")


if __name__ == '__main__':
    unittest.main()
