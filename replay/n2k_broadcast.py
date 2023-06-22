import argparse
import glob
import gzip
import os
import socket  # for UDP broadcast
import threading
from datetime import datetime
from time import sleep
from pynput import keyboard
from pynput.keyboard import KeyCode
from http.server import BaseHTTPRequestHandler, HTTPServer

from nmea_sim import make_kml
from nmeaparser import NmeaParser

NMEA0183_PREFIX = 'N2K_NMEA0183,'
NMEA0183_PREFIX_LEN = len(NMEA0183_PREFIX)

kml_place_mark = ''
run_http_server = True


# noinspection PyPep8Naming
class RequestHandler(BaseHTTPRequestHandler):
    def __init__(self, request, client_address, server):
        BaseHTTPRequestHandler.__init__(self, request, client_address, server)

    def log_message(self, format, *args):
        return

    def do_OPTIONS(self):
        """
        This method is called when HTTP server receives OPTIONS request
        :return: None
        """
        print('Processing OPTIONS request to allow CORS')
        self.send_response(200)
        self.send_header('Access-Control-Allow-Headers',
                         'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token')
        self.send_header('Access-Control-Allow-Methods', 'GET,POST,OPTIONS,PUT,PATCH,DELETE')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

    def do_GET(self):
        """
        This method is called when HTTP server receives GET request
        :return: None
        """
        self.send_response(200)
        self.send_header('Content-Type', 'application/vnd.google-earth.kml+xml')
        self.end_headers()
        self.wfile.write(kml_place_mark.encode())


def start_http_server(http_port):
    x = threading.Thread(target=http_thread, args=(http_port,))
    x.start()
    return x


def http_thread(http_port):
    print(f'Starting HTTP server on port {http_port}')
    server = None
    try:
        # Create a web server and define the handler to manage the
        # incoming request
        server = HTTPServer(('', http_port), RequestHandler)
        server.timeout = 1
        print('Started http server on port ', http_port)

        while run_http_server:
            server.handle_request()

    except KeyboardInterrupt:
        if server:
            print('^C received, shutting down the web server')
            server.socket.close()

    print('HTTP thread stopped')

def make_can_frame(can_line):
    t = can_line.split(' ')
    can_id = int(t[0], 16).to_bytes(4, byteorder='big')
    can_dlc = (len(t) - 1).to_bytes(1, byteorder='big')
    can_data = bytearray.fromhex(''.join(t[1:]))
    can_frame = can_id + can_dlc + can_data

    return can_frame


def main(args):
    global kml_place_mark, run_http_server
    http_thr = None
    if args.http_port is not None:
        http_thr = start_http_server(args.http_port)

    nmea_parser = NmeaParser(None)

    log_list = []
    log_list += sorted(glob.glob(args.replay_dir + os.sep + '*-log.txt'))
    log_list += sorted(glob.glob(args.replay_dir + os.sep + '*.log'))

    # Create broadcast UPD socket

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    paused = False
    skip_to_ts = None
    with keyboard.Events() as events:
        prev_msg_time_stamp = None
        for log in log_list:
            if log.endswith('.gz'):
                f = gzip.open(log, 'rt')
            else:
                f = open(log, 'rt')
            print('Replaying {}'.format(log))
            print('Press SPACE to pause, q to quit, right to skip one minute, page down to skip 10 minutes')
            while True:
                if not paused:
                    line = f.readline()
                    if line.find('RAW_N2K,') > 0 or line.find('UDP_N2K,') > 0:
                        # Date and time are the first two fields separated by a space
                        date = line.split(' ')[0]
                        time = line.split(' ')[1]
                        dt = datetime.strptime(str(date) + ' ' + str(time), '%Y-%m-%d %H:%M:%S.%f')
                        # Convert to Unix time
                        msg_time_stamp = dt.timestamp()

                        if skip_to_ts is not None:
                            if msg_time_stamp < skip_to_ts:
                                print(f'\r skipping {time} {msg_time_stamp} < {skip_to_ts}', end='')
                                continue
                            else:
                                skip_to_ts = None
                                prev_msg_time_stamp = msg_time_stamp

                        # Time to sleep before sending this message
                        if prev_msg_time_stamp is not None:
                            sleep_time = msg_time_stamp - prev_msg_time_stamp
                            sleep(sleep_time)
                        prev_msg_time_stamp = msg_time_stamp

                        idx = line.find(' R ')
                        if idx > 0:
                            can_line = line[idx + 3:-2]
                            can_frame = make_can_frame(can_line)
                            # print('Sending: {}'.format(can_line))
                            print(f'\r {time}', end='')
                            s.sendto(can_frame, ('<broadcast>', args.udp_port))
                    elif line.find(NMEA0183_PREFIX) > 0:
                        idx = line.find(NMEA0183_PREFIX) + NMEA0183_PREFIX_LEN
                        nmea = line[idx:]
                        nmea_parser.set_nmea_sentence(nmea)
                        if "RMC," in nmea:
                            kml_place_mark = make_kml(nmea_parser)

                event = events.get(0)
                if event is not None:
                    # print('Received event {}'.format(event))
                    if isinstance(event, keyboard.Events.Release):
                        if event.key == keyboard.Key.space:  # Pause
                            paused = not paused
                            print('Paused') if paused else print('Resumed')
                        elif event.key == keyboard.Key.right:  # skip 1 minute
                            skip_to_ts = msg_time_stamp + 60
                        elif event.key == keyboard.Key.page_down:  # skip 10 minutes
                            skip_to_ts = msg_time_stamp + 600
                        elif isinstance(event.key, KeyCode) and event.key.char is not None:  # Quit
                            if event.key.char == 'q':
                                break

    if http_thr is not None:
        print('Stopping http server')
        run_http_server = False
        http_thr.join()
        print('http server stopped')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--udp-port", help="UDP port to broadcast data", default=2024)
    parser.add_argument("--http-port", help="HTTP port ", required=False, type=int)
    parser.add_argument("--replay-dir", help="Replay logs found in this directory", required=True)
    main(parser.parse_args())
