import argparse
import os
import selectors
import socket
import threading
import serial
import connexion

import conf
from navcomputer.Logger import Logger
from nmea_interface import NmeaInterface
from nmeaparser import NmeaParser
from data_registry import DataRegistry
from flask_cors import CORS


def accept_nmea_tcp(sock, sel, interfaces, nmea_parser, instr_inputs):
    conn, addr = sock.accept()  # Should be ready
    print('accepted', conn, 'from', addr)
    conn.setblocking(False)
    conn.send(bytes('Hello', 'utf-8'))
    interface = NmeaInterface(conn, NmeaInterface.TCP_APP_CLIENTS, nmea_parser, instr_inputs)
    sel.register(conn, selectors.EVENT_READ, interface)
    interfaces.append(interface)


def read_serial(port, sel):
    pass


def add_serial_port(sel, inp, interfaces, nmea_parser):
    print('Adding input {} ...'.format(inp))
    t = inp.split(':')
    if len(t) != 2:
        print('Wrong serial port input format: {}'.format(inp))
        print('Must be /dev/ttyAMA:4800 for /dev/ttyAMA at 4800 baud')
        return

    port_name = t[0]
    baud_rate = int(t[1])
    try:
        ser = serial.Serial(port_name, baud_rate, timeout=None)
        interface = NmeaInterface(ser, NmeaInterface.SERIAL_INSTRUMENTS, nmea_parser, [])
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
    sock.bind(('localhost', tcp_port))
    sock.listen(100)
    sock.setblocking(False)
    sel.register(sock, selectors.EVENT_READ, accept_nmea_tcp)
    print('Listening on port {} for NMEA TCP connections ...'.format(tcp_port))


def flask_server(http_port):
    app = connexion.App(__name__, specification_dir='openapi/')
    app.add_api('ottopi.yaml')
    CORS(app.app)

    # Use FLASK development server to host connexion app
    app.run(port=http_port)


def start_flask_server(http_port):
    # noinspection PyRedundantParentheses
    t = threading.Thread(target=flask_server, name='flask_server', args=[http_port])
    t.start()


def main(args):

    Logger.set_log_dir(args.log_dir)
    print('Inputs', args.inputs)

    sel = selectors.DefaultSelector()
    interfaces = []
    data_registry = DataRegistry.get_instance()
    data_registry.read_gpx_file(conf.DATA_DIR + os.sep + conf.GPX_ARCHIVE_NAME)
    data_registry.restore_active_route()

    nmea_parser = NmeaParser(data_registry)

    # Add TCP server
    add_tcp_server(sel, int(args.tcp_server_port))

    # Open and register specified inputs
    instr_inputs = []
    for inp in args.inputs:
        if inp.startswith('tcp'):
            ifc = add_tcp_client(sel, inp, interfaces, nmea_parser)
            if ifc is not None:
                instr_inputs.append(ifc)
        elif inp.startswith('/dev/tty'):
            ifc = add_serial_port(sel, inp, interfaces, nmea_parser)
            if ifc is not None:
                instr_inputs.append(ifc)

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
    parser.add_argument("--inputs", help="List of inputs ", nargs='*',  required=True)
    parser.add_argument("--tcp-server-port", help="TCP port for incoming connections", required=True)
    parser.add_argument("--http-server-port", help="HTTP server port", required=True)
    main(parser.parse_args())
