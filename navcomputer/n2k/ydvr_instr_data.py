""" Converts YDVR .DAT files to RawInstrData data"""
import glob
import json
import os
from datetime import datetime, timezone
import subprocess

from raw_instr_data import RawInstrData

MF_PGNS = [
    65240,  126208, 126464, 126720, 126983, 126988, 126996, 126998, 127233, 127237, 127489, 127496, 127498, 127503,
    127504, 127506, 127507, 127509, 127514, 128275, 128520, 129029, 129038, 129041, 129044, 129045, 129284, 129285,
    129301, 129302, 129538, 129540, 129542, 129545, 129547, 129549, 129551, 129556, 129792, 129810, 130052, 130054,
    130060, 130061, 130064, 130074, 130320, 130324, 130567, 130577, 130578, 130816
]

CAN_ANALYZER_BIN = '/Users/sergei/github/canboat/rel/darwin-x86_64/analyzer'


class YdvrDecoder:
    def __init__(self):
        self.last_ts_ms = 0
        self.start_timestamp = None
        self.temp_ii = RawInstrData()
        self.instr_data = []

    @staticmethod
    def can_id_to_n2k(can_id):
        can_id_pf = (can_id >> 16) & 0x00FF
        can_id_ps = (can_id >> 8) & 0x00FF
        can_id_dp = (can_id >> 24) & 1

        src = can_id >> 0 & 0x00FF
        prio = ((can_id >> 26) & 0x7)

        if can_id_pf < 240:
            # /* PDU1 format, the PS contains the destination address */
            dst = can_id_ps
            pgn = (can_id_dp << 16) | (can_id_pf << 8)
        else:
            # /* PDU2 format, the destination is implied global and the PGN is extended */
            dst = 0xff
            pgn = (can_id_dp << 16) | (can_id_pf << 8) | can_id_ps

        return prio, pgn, src, dst

    def get_time(self, timestamp_ms, pgn, data):
        if pgn == 129029:  # Set time from GNSS Position Data PGN
            fix_quality = int.from_bytes(data[31:32], byteorder='little') >> 4
            if fix_quality > 0:  # Wait for valid GPS fix
                pos_date = int.from_bytes(data[1:3], byteorder='little')
                pos_time = int.from_bytes(data[3:7], byteorder='little')
                self.start_timestamp = pos_date * 24 * 3600 + pos_time / 10000. - timestamp_ms / 1000
                # print('Got GNSS Position Data')
        if self.start_timestamp is None:
            return None
        else:
            return datetime.utcfromtimestamp(self.start_timestamp + timestamp_ms / 1000)

    def process_msg(self, timestamp, prio, pgn, src, dst, data):
        dt_object = self.get_time(timestamp, pgn, data)
        if dt_object is not None:
            dlen = len(data)
            dt_str = dt_object.strftime("%Y-%m-%d:%H:%M:%S.%f")
            s = f'{dt_str},{prio},{pgn},{src},{dst},{dlen},{data.hex(",")}'
            # print(s)
            return s, dt_object.timestamp()
        else:
            return None, None

    def ydvr_to_ii(self, ydvr_dir):
        """
        :param ydvr_dir: Directory containing YDVR .DAT files
        :return: list of RawInstrData objects
        """

        self.instr_data = []
        # Get list of *.DAT files in directory
        dat_files = glob.glob(os.path.join(ydvr_dir, '*.DAT'))
        dat_files.sort()
        can_proc = subprocess.Popen([CAN_ANALYZER_BIN, '-json'], stdin=subprocess.PIPE,
                                    stdout=subprocess.PIPE,
                                    text=True,  # Use text mode for easier communication (Python 3.7+)
                                    bufsize=1,  # Line-buffered mode for stdout
                                    universal_newlines=True
                                    )

        for dat_file in dat_files:
            with open(dat_file, 'rb') as f:
                # First two bytes are time stamp
                while True:
                    ts_bytes = f.read(2)
                    if len(ts_bytes) < 2:
                        break
                    ts = int.from_bytes(ts_bytes, byteorder='little')
                    if ts < self.last_ts_ms:
                        ts += 60000
                    msg_id_bytes = f.read(4)
                    if len(msg_id_bytes) < 4:
                        break
                    msg_id = int.from_bytes(msg_id_bytes, byteorder='little')
                    if msg_id == 0xFFFFFFFF:  # YDVR Service record
                        data = f.read(8)
                        if len(data) < 8:
                            break
                        if data[0:4] == b'YDVR':  # Start record
                            # Reset time
                            self.start_timestamp = None
                        elif data[0] == b'E'[0]:  # End record
                            # Reset time
                            self.start_timestamp = None
                        elif data[0] == b'T'[0]:  # record between messages between which more than 1 minute has passed
                            # Reset time later one my do something fancier
                            self.start_timestamp = None
                    else:
                        prio, pgn, src, dst = self.can_id_to_n2k(msg_id)
                        if pgn == 59904:
                            data = f.read(3)
                            if len(data) < 3:
                                break
                        elif pgn in MF_PGNS:
                            f.read(1)  # Sequence number
                            data_len = int.from_bytes(f.read(1), byteorder='little')
                            data = f.read(data_len)
                            if len(data) < data_len:
                                break
                        else:
                            data = f.read(8)
                            if len(data) < 8:
                                break
                        s, msg_ts = self.process_msg(ts, prio, pgn, src, dst, data)
                        if s is not None:
                            can_proc.stdin.write(s + '\n')
                            can_proc.stdin.flush()
                            out_line = can_proc.stdout.readline()
                            if out_line is not None:
                                json_data = json.loads(out_line)
                                self.process_pgn(json_data)

        can_proc.stdin.close()
        can_proc.stdout.close()
        can_proc.wait()
        return self.instr_data

    @staticmethod
    def mps_to_kts(mps):
        # Convert meters per second to knots
        return mps * 1.94384449

    @staticmethod
    def signed_deg(deg):
        # Convert unsigned degrees to signed degrees
        if deg > 180:
            deg -= 360

        return deg

    def process_pgn(self, msg):
        # print(msg)
        if not ('fields' in msg and 'pgn' in msg):
            return
        f = msg['fields']
        pgn = msg['pgn']
        if pgn == 129029:
            if f['Method'] != "no GNSS":
                self.temp_ii.lat = float(f['Latitude'])
                self.temp_ii.lon = float(f['Longitude'])
                # Parse datetime  from format lie '2023-07-16:17:21:57.000000'
                self.temp_ii.utc = datetime.strptime(msg['timestamp'], "%Y-%m-%d:%H:%M:%S.%f")
                # Set UTC timezone to datetime object
                self.temp_ii.utc = self.temp_ii.utc.replace(tzinfo=timezone.utc)
                # print(f'{self.temp_ii.to_dict()}')
                self.instr_data.append(self.temp_ii)
                self.temp_ii = RawInstrData()
        elif pgn == 129026:
            self.temp_ii.sog = self.mps_to_kts(f['SOG']) if 'SOG' in f else None
            self.temp_ii.cog = f['COG'] if 'COG' in f else None
        elif pgn == 127250:
            self.temp_ii.hdg = f['Heading']
        elif pgn == 130306:
            if f['Reference'] == 'Apparent':
                self.temp_ii.awa = self.signed_deg(f['Wind Angle']) if 'Wind Angle' in f else None
                self.temp_ii.aws = self.mps_to_kts(f['Wind Speed']) if 'Wind Speed' in f else None
            elif f['Reference'] == 'True (water referenced)':
                self.temp_ii.twa = self.signed_deg(f['Wind Angle']) if 'Wind Angle' in f else None
                self.temp_ii.tws = self.mps_to_kts(f['Wind Speed']) if 'Wind Speed' in f else None
        elif pgn == 128259:
            self.temp_ii.sow = self.mps_to_kts(f['Speed Water Referenced']) if 'Speed Water Referenced' in f else None


if __name__ == '__main__':
    YdvrDecoder().ydvr_to_ii('/Volumes/G-DRIVE mobile USB-C/2023 YDVR Trial/YDVR0001')
