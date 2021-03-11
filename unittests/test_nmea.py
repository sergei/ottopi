import unittest
import datetime

from gpxpy.gpx import GPXRoutePoint

from navigator_listener import DestInfo
import nmea_encoder
from nmeaparser import NmeaParser
from raw_instr_data import RawInstrData


class TestNmea(unittest.TestCase):

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

    def test_parse_vwr(self):
        nmea = '$IIVWR,039,R,11.2,N,05.7,M,020.8,K'
        nmea_parser = NmeaParser(None)
        nmea_parser.set_nmea_sentence(nmea)
        self.assertAlmostEqual(nmea_parser.awa, 39, delta=0.1)
        self.assertAlmostEqual(nmea_parser.aws, 11.2, delta=0.1)

    def test_parse_rmc(self):
        nmea = '$GPRMC,,V,,,,,,,,,,N*53'
        nmea_parser = NmeaParser(None)
        nmea_parser.set_nmea_sentence(nmea)
        self.assertIsNone(nmea_parser.utc)
        self.assertIsNone(nmea_parser.lat)
        self.assertIsNone(nmea_parser.lon)
        self.assertIsNone(nmea_parser.sog)
        self.assertIsNone(nmea_parser.cog_true)


if __name__ == '__main__':
    unittest.main()
