import argparse
import selectors
import socket
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

import serial

from nmeaparser import NmeaParser

sel = selectors.DefaultSelector()
connections = []
serial_ports = []


def accept(sock, mask):
    conn, addr = sock.accept()  # Should be ready
    print('accepted', conn, 'from', addr)
    conn.setblocking(False)
    conn.send(bytes('Hello', 'utf-8'))
    sel.register(conn, selectors.EVENT_READ, read_tcp)
    connections.append(conn)


def read_tcp(conn, mask):
    still_connected = True
    try:
        data = conn.recv(1000)  # Should be ready
        if data:
            print('received', repr(data), 'from', conn)
        else:
            print('closing', conn)
            still_connected = False
    except ConnectionResetError as e:
        print(e)
        still_connected = False

    if not still_connected:
        print('closing', conn)
        sel.unregister(conn)
        conn.close()
        connections.remove(conn)


def read_serial(port, mask):
    still_connected = True
    try:
        data = port.read(1)  # Should be ready
        if data:
            print(data.decode('ascii', errors='ignore'), end='')
        else:
            print('closing', port)
            still_connected = False
    except ConnectionResetError as e:
        print(e)
        still_connected = False

    if not still_connected:
        print('closing', port)
        sel.unregister(port)
        port.close()
        serial_ports.remove(port)


last_rmc_time_tag = None
kml_place_mark = ''


def make_kml(nmea):
    kml = f"""<?xml version="1.0" encoding="UTF-8"?>
    <kml xmlns="http://www.opengis.net/kml/2.2"
     xmlns:gx="http://www.google.com/kml/ext/2.2" 
     xmlns:kml="http://www.opengis.net/kml/2.2" 
     xmlns:atom="http://www.w3.org/2005/Atom">
    <Document>
    	<name>KmlFile</name>
    	<Style id="s_ylw-pushpin">
    		<IconStyle>
    			<scale>1.2</scale>
    			<heading>{nmea.cog_true}</heading>    
    			<Icon>
    				<href>http://maps.google.com/mapfiles/kml/shapes/track.png</href>
    			</Icon>
    			<hotSpot x="32" y="1" xunits="pixels" yunits="pixels"/>
    		</IconStyle>
    		<ListStyle>
    		</ListStyle>
    	</Style>
    	<Placemark>
    		<name>Boat</name>
    		<description>{nmea.utc} AWA: {nmea.awa} AWS: {nmea.aws} </description>
    		<styleUrl>#s_ylw-pushpin</styleUrl>
    		<Point>
    			<coordinates>{nmea.lon},{nmea.lat}</coordinates>
    		</Point>
    	</Placemark>
    </Document>
    </kml>
    """

    return kml


def time_tagged_line(raw_line):
    global last_rmc_time_tag, kml_place_mark
    raw_line = raw_line.decode('ascii', errors='ignore')
    if "RMC," in raw_line:
        nmea_parser = NmeaParser(None)
        nmea_parser.set_nmea_sentence(raw_line)
        unix_time_ms = int(nmea_parser.utc.timestamp() * 1000)
        last_rmc_time_tag = f'\\c:{unix_time_ms}\\'
        kml_place_mark = make_kml(nmea_parser)

    if last_rmc_time_tag is None:
        return None

    if raw_line.startswith("$G"):
        return raw_line.encode()
    else:
        return (last_rmc_time_tag + raw_line).encode()


def http_thread(http_port):
    print(f'Starting HTTP server on port {http_port}')
    server = None
    try:
        # Create a web server and define the handler to manage the
        # incoming request
        server = HTTPServer(('', http_port), RequestHandler)
        print('Started http server on port ', http_port)

        while True:
            server.handle_request()

    except KeyboardInterrupt:
        if server:
            print('^C received, shutting down the web server')
            server.socket.close()


def start_http_server(http_port):
    x = threading.Thread(target=http_thread, args=(http_port,))
    x.start()


def nmea_sim(args):
    if args.http_port is not None:
        start_http_server(args.http_port)

    with open(args.nmea_file, 'rb') as nmea_file:
        if args.tcp_port is not None:
            sock = socket.socket()
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.bind(('0.0.0.0', int(args.tcp_port)))
            sock.listen(100)
            sock.setblocking(False)
            sel.register(sock, selectors.EVENT_READ, accept)
            print('Listening on port {}'.format(args.tcp_port))

        if args.serial_port is not None:
            try:
                ser = serial.Serial(args.serial_port, 4800, timeout=None,  bytesize=8, parity='N',
                                    stopbits=1, xonxoff=0, rtscts=0)

                sel.register(ser, selectors.EVENT_READ, read_serial)
                serial_ports.append(ser)

            except serial.serialutil.SerialException:
                print('Failed to open {}'.format(args.serial_port))
                return None

        last_epoch = 0
        while True:
            events = sel.select(timeout=0)
            for key, mask in events:
                callback = key.data
                callback(key.fileobj, mask)

            # Read and send NMEA epoch
            now = time.time()
            if now - last_epoch > 1:
                last_epoch = now
                for raw_line in nmea_file:
                    line = time_tagged_line(raw_line)
                    if line is None:
                        continue

                    chunk_size = 10
                    for i in range(0, len(line), chunk_size):
                        chunk_end = i + chunk_size
                        if chunk_end > len(line):
                            chunk_end = len(line)
                        for conn in connections:
                            conn.send(line[i:chunk_end])
                        for port in serial_ports:
                            port.write(line[i:chunk_end])
                    if b'RMC,' in line:
                        break


class RequestHandler(BaseHTTPRequestHandler):
    def __init__(self, request, client_address, server):
        BaseHTTPRequestHandler.__init__(self, request, client_address, server)

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


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Utility to playback NMEA file", fromfile_prefix_chars='@')
    parser.add_argument("--nmea-file", help="NMEA file", required=True)
    parser.add_argument("--tcp-port", help="TCP port", required=False)
    parser.add_argument("--serial-port", help="Serial port port", required=False)
    parser.add_argument("--http-port", help="HTTP port port", required=False, type=int)
    nmea_sim(parser.parse_args())
