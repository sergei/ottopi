import argparse
import datetime
import os
import selectors
import socket
import threading
import serial
import connexion
from flask_cors import CORS
from flask_sockets import Sockets
import logging

from sys import platform

from sk_client import SkClient

if platform == "darwin":
    BT_CONF_DIR = 'BT_CONF_DIR'
else:
    from bt_rest_api import BT_CONF_DIR

from logger import Logger
import conf
from nmea_interface import NmeaInterface
from nmeaparser import NmeaParser
from speaker import Speaker
from navigator import Navigator


def accept_nmea_tcp(sock, sel, interfaces, nmea_parser, instr_inputs):
    conn, addr = sock.accept()  # Should be ready
    print('accepted', conn, 'from', addr)
    conn.setblocking(False)
    conn.send(bytes('Hello', 'utf-8'))
    interface = NmeaInterface(conn, NmeaInterface.TCP_APP_CLIENTS, nmea_parser, instr_inputs)
    sel.register(conn, selectors.EVENT_READ, interface)
    interfaces.append(interface)


def add_serial_port(sel, inp, interfaces, nmea_parser):
    print('Adding input {} ...'.format(inp))
    t = inp.split(':')
    if len(t) != 3:
        print('Wrong serial port input format: {}'.format(inp))
        print('Must be /dev/ttyUSB0:4800:gps or /dev/ttyS0:4800:instr')
        return

    port_name = t[0]
    baud_rate = int(t[1])
    try:
        ser = serial.Serial(port_name, baud_rate, timeout=None)
        if t[2] == 'gps':
            ifc_type = NmeaInterface.SERIAL_NMEA_GPS
        elif t[2] == 'instr':
            ifc_type = NmeaInterface.SERIAL_NMEA_INSTR
        elif t[2] == 'b&g':
            # Since GPS migh not be available we need to set UTC to open the log file
            now = datetime.datetime.now()
            Logger.set_utc(now)
            ifc_type = NmeaInterface.SERIAL_BANG_NET
        else:
            print('Wrong serial port input type: {}'.format(inp))
            return

        interface = NmeaInterface(ser, ifc_type, nmea_parser, [])
        sel.register(ser, selectors.EVENT_READ, interface)
        interfaces.append(interface)
        return interface
    except serial.serialutil.SerialException:
        print('Failed to open {}'.format(port_name))
        return None


def add_tcp_client(sel, inp, interfaces, nmea_parser):
    print('Adding input {} ...'.format(inp))
    t = inp.split(':')
    if len(t) != 3:
        print('Wrong tcp client input format: {}'.format(inp))
        print('Must be tcp:localhost:12345')
        return

    host_name = t[1]
    port = int(t[2])
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((host_name, port))
    interface = NmeaInterface(sock, NmeaInterface.TCP_INSTRUMENTS_INPUT, nmea_parser, [])
    sel.register(sock, selectors.EVENT_READ, interface)
    interfaces.append(interface)
    return interface


def add_tcp_server(sel, tcp_port):
    sock = socket.socket()
    sock.bind(('0.0.0.0', tcp_port))
    sock.listen(100)
    sock.setblocking(False)
    sel.register(sock, selectors.EVENT_READ, accept_nmea_tcp)
    print('Listening on port {} for NMEA TCP connections ...'.format(tcp_port))


def flask_server(http_port):
    app = connexion.App(__name__, specification_dir='openapi/')
    app.add_api('ottopi.yaml')
    CORS(app.app)
    sockets = Sockets(app.app)

    @sockets.route('/signalk/v1/stream')
    def echo_socket(ws):
        message = ws.receive()
        print(message)

    log = logging.getLogger('werkzeug')
    log.setLevel(logging.ERROR)

    # Use FLASK development server to host connexion app
    # app.run(port=http_port, debug=False)

    # Use pywsgi to host connexion app and websockets
    from gevent import pywsgi
    from geventwebsocket.handler import WebSocketHandler
    server = pywsgi.WSGIServer(('', http_port), app, handler_class=WebSocketHandler)
    server.serve_forever()


def start_flask_server(http_port):
    # noinspection PyRedundantParentheses
    t = threading.Thread(target=flask_server, name='flask_server', args=[http_port], daemon=True)
    t.start()


def main(args):
    print('Inputs', args.inputs)
    Logger.set_log_dir(args.log_dir)

    # This variable is used by BT rest APIs to load BT configuration
    os.environ[BT_CONF_DIR] = args.data_dir

    navigator = Navigator.get_instance()
    data_dir = os.path.expanduser(args.data_dir)
    navigator.set_data_dir(data_dir)
    gpx_file_name = navigator.get_data_dir() + os.sep + conf.GPX_ARCHIVE_NAME
    navigator.read_gpx_file(gpx_file_name)
    navigator.restore_active_route()
    navigator.read_polars(data_dir + os.sep + conf.POLAR_NAME)

    nmea_parser = NmeaParser(navigator, args.strict_nmea)

    if args.replay_dir is not None:
        from replay import Replay
        replay = Replay(args.replay_dir, args.log_dir, nmea_parser)
        navigator.add_listener(replay)
        replay.run(args.with_prefix, args.signalk, args.replay_start, args.replay_days)
        return

    navigator.add_listener(Speaker.get_instance())
    inputs = []
    if args.inputs is not None:
        for s in args.inputs:
            inputs += s.split()

    sel = selectors.DefaultSelector()
    interfaces = []

    # Add TCP server
    if args.tcp_server_port is not None:
        add_tcp_server(sel, int(args.tcp_server_port))

    # Open and register specified inputs
    instr_inputs = []
    for inp in inputs:
        if inp.startswith('tcp'):
            ifc = add_tcp_client(sel, inp, interfaces, nmea_parser)
            if ifc is not None:
                instr_inputs.append(ifc)
        elif inp.startswith('/dev/tty'):
            ifc = add_serial_port(sel, inp, interfaces, nmea_parser)
            if ifc is not None:
                instr_inputs.append(ifc)
        elif inp.startswith('signalk'):
            t = inp.split('|')
            client = SkClient(t[1])
            client.add_listener(navigator)
            client.subscribe()

    # Connect inputs to outputs
    for inp_ifc in instr_inputs:
        for out_ifc in instr_inputs:
            if out_ifc.interface_type in [NmeaInterface.SERIAL_NMEA_INSTR]:
                inp_ifc.add_nmea_listener(out_ifc)

    # Start FLASK server
    start_flask_server(int(args.http_server_port))

    # Start polling the inputs
    while True:
        events = sel.select(timeout=1)
        for key, mask in events:
            if key.data == accept_nmea_tcp:
                accept_nmea_tcp(key.fileobj, sel, interfaces, nmea_parser, instr_inputs)
            else:
                read_interface = key.data
                received_data = read_interface.read()
                if received_data is None:  # Must be
                    sel.unregister(read_interface.file)
                    read_interface.file.close()
                    interfaces.remove(read_interface)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--log-dir", help="Directory to store logs",  required=True)
    parser.add_argument("--data-dir", help="Directory to keep GPX data",  required=True)
    parser.add_argument("--inputs", help="List of inputs ", nargs='*',  required=False)
    parser.add_argument("--tcp-server-port", help="TCP port for incoming connections", required=False)
    parser.add_argument("--http-server-port", help="HTTP server port", required=False)
    parser.add_argument("--replay-dir", help="Replay logs found in this directory", required=False)
    parser.add_argument("--with-prefix", help="Analyze prefix in the files being replayed", default=False,
                        action='store_true', required=False)
    parser.add_argument("--strict-nmea", help="NMEA checksum is not optional", default=False,
                        action='store_true', required=False)
    parser.add_argument("--signalk", help="Log collected from SignalK", default=False,
                        action='store_true', required=False)
    parser.add_argument("--replay-start", help="Date to start replay from YYYY-MM-DD UTC",
                        type=datetime.date.fromisoformat, required=False)
    parser.add_argument("--replay-days", help="How many days to replay", default=1, required=False)

    main(parser.parse_args())
