import argparse
import selectors
import socket
import serial

import connexion
from wsgiref.simple_server import make_server


class Interface:
    SERIAL = 0
    INCOMING_TCP = 1
    OUTGOING_TCP = 2

    def __init__(self, file, interface_type):
        self.file = file
        self.interface_type = interface_type

    def read(self):
        if self.interface_type == self.SERIAL:
            data = self.file.read(1000)  # Should be ready
            if data:
                print('received', repr(data), 'from', self.file)
                return data
            else:
                print('Lost connection to ', self.file)
                return None
        else:
            data = self.file.recv(1000)  # Should be ready
            if data:
                print('received', repr(data), 'from', self.file)
                return data
            else:
                print('Lost connection to ', self.file)
                return None


def read_tcp(port, sel):
    pass


def accept_nmea_tcp(sock, sel, interfaces):
    conn, addr = sock.accept()  # Should be ready
    print('accepted', conn, 'from', addr)
    conn.setblocking(False)
    conn.send(bytes('Hello', 'utf-8'))
    interface = Interface(conn, Interface.INCOMING_TCP)
    sel.register(conn, selectors.EVENT_READ, interface)
    interfaces.append(interface)


def accept_http_request(sock, http_server):
    request, client_address = sock.accept()  # Should be ready
    http_server.process_request(request, client_address)


def read_serial(port, sel):
    pass


def add_serial_port(sel, inp, interfaces):
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
        interface = Interface(ser, Interface.SERIAL)
        sel.register(ser, selectors.EVENT_READ, interface)
        interfaces.append(interface)
    except serial.serialutil.SerialException:
        print('Failed to open {}'.format(port_name))
        return


def add_tcp_client(sel, inp, interfaces):
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
    interface = Interface(sock, Interface.OUTGOING_TCP)
    sel.register(sock, selectors.EVENT_READ, interface)
    interfaces.append(interface)


def add_tcp_server(sel, tcp_port):
    sock = socket.socket()
    sock.bind(('localhost', tcp_port))
    sock.listen(100)
    sock.setblocking(False)
    sel.register(sock, selectors.EVENT_READ, accept_nmea_tcp)
    print('Listening on port {} for NMEA TCP connections ...'.format(tcp_port))


def add_http_port(sel, http_port):
    sock = socket.socket()
    sock.bind(('localhost', http_port))
    sock.listen(100)
    sock.setblocking(False)
    sel.register(sock, selectors.EVENT_READ, accept_http_request)
    print('Listening on port {} for HTTP connections ...'.format(http_port))


def nmea_bridge(args):
    print('Inputs', args.inputs)

    sel = selectors.DefaultSelector()
    interfaces = []

    # Add TCP server
    add_tcp_server(sel, int(args.tcp_server_port))

    # Create connexion App to serve REST APIs
    app = connexion.App(__name__, specification_dir='openapi/')
    app.add_api('ottopi.yaml')

    # Add HTTP server
    add_http_port(sel, int(args.http_server_port))
    http_server = make_server('', int(args.http_server_port), app)

    # Open and register specified inputs
    for inp in args.inputs:
        if inp.startswith('tcp'):
            add_tcp_client(sel, inp, interfaces)
        elif inp.startswith('/dev/tty'):
            add_serial_port(sel, inp, interfaces)

    # Start polling the inputs

    while True:
        events = sel.select(timeout=1)
        for key, mask in events:
            if key.data == accept_nmea_tcp:
                accept_nmea_tcp(key.fileobj, sel, interfaces)
            elif key.data == accept_http_request:
                accept_http_request(key.fileobj, http_server)
            else:
                read_interface = key.data
                received_data = read_interface.read()
                if received_data is None:  # Must be
                    sel.unregister(read_interface.file)
                    read_interface.file.close()
                    interfaces.remove(read_interface)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--inputs", help="List of inputs ", nargs='*',  required=True)
    parser.add_argument("--tcp-server-port", help="TCP port for incoming connections", required=True)
    parser.add_argument("--http-server-port", help="HTTP server port", required=True)
    nmea_bridge(parser.parse_args())
