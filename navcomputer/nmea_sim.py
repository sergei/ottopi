import argparse
import selectors
import socket
import time

import serial

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


def nmea_sim(args):
    with open(args.nmea_file, 'rb') as nmea_file:
        if args.tcp_port is not None:
            sock = socket.socket()
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
                for line in nmea_file:
                    chunk_size = 10
                    for i in range(0, len(line), chunk_size):
                        chunk_end = i + chunk_size
                        if chunk_end > len(line):
                            chunk_end = len(line)
                        for conn in connections:
                            conn.send(line[i:chunk_end])
                        for port in serial_ports:
                            port.write(line[i:chunk_end])
                    if b'$GPRMC' in line:
                        break


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Utility to playback NMEA file", fromfile_prefix_chars='@')
    parser.add_argument("--nmea-file", help="NMEA file", required=True)
    parser.add_argument("--tcp-port", help="TCP port", required=False)
    parser.add_argument("--serial-port", help="Serial port port", required=False)
    nmea_sim(parser.parse_args())
