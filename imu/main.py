import argparse
import os
import selectors
import socket
import threading
import time
from functools import reduce

from fusion import Fusion
from raw_sensors import RawSensors
from time_diff import time_diff

sel = selectors.DefaultSelector()
tcp_nmea_connections = []


# noinspection PyUnusedLocal
def read(conn, mask):
    data = conn.recv(1)
    if not data:
        print('closing', conn)
        sel.unregister(conn)
        conn.close()
        tcp_nmea_connections.remove(conn)


def append_nmea_checksum(nmea):
    cc = reduce(lambda i, j: int(i) ^ int(j), [ord(x) for x in nmea[1:]])  # Exclude $ sign
    return nmea + '*{:02X}'.format(cc)


def send_xdr(heading, pitch, roll):
    nmea = f'$OPXDR,A,{heading:.1f},D,YAW,A,{pitch:.1f},D,PTCH,A,{roll:.1f},D,ROLL'
    nmea = bytes(append_nmea_checksum(nmea) + '\r\n', 'ascii')

    for conn in tcp_nmea_connections:
        conn.send(nmea)


# noinspection PyUnusedLocal
def accept_nmea_tcp(sock, mask):
    conn, addr = sock.accept()  # Should be ready
    print('accepted', conn, 'from', addr)
    conn.setblocking(False)
    tcp_nmea_connections.append(conn)
    sel.register(conn, selectors.EVENT_READ, read)


def wait_for_connections():
    while True:
        events = sel.select()
        for key, mask in events:
            callback = key.data
            callback(key.fileobj, mask)


def add_tcp_server(tcp_port):
    sock = socket.socket()
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', tcp_port))
    sock.listen(100)
    sock.setblocking(False)
    sel.register(sock, selectors.EVENT_READ, accept_nmea_tcp)
    print('Listening on port {} for NMEA 0183 TCP connections ...'.format(tcp_port))
    t = threading.Thread(target=wait_for_connections, name='tcp_server', daemon=True)
    t.start()


def main(args):
    raw = RawSensors()
    fusion = Fusion(timediff=time_diff)

    if args.tcp_server_port is not None:
        add_tcp_server(int(args.tcp_server_port))

    csv_log_name = os.path.expanduser(args.log_dir + os.sep + 'sensors.csv') if args.log_dir is not None else None
    csv_file = open(csv_log_name, 'wt') if csv_log_name is not None else None
    if csv_file is not None:
        csv_file.write('t,ax,ay,az,gx,gy,gz,mx,my,mz,heading,pitch,roll\n')
    while True:
        try:
            accel, gyro, mag, t = raw.get_raw_data()
            fusion.update(accel, gyro, mag, t)
            # print('heading {:.3f}, pitch {:.3f}, roll {:.3f}'.format(fusion.heading, fusion.pitch, fusion.roll))
            send_xdr(fusion.heading, fusion.pitch, fusion.roll)
            if csv_file is not None:
                csv_file.write('{},{},{},{},{},{},{},{},{},{},{},{},{}\n'.format(t,
                                                                                 accel[0], accel[1], accel[2],
                                                                                 gyro[0], gyro[1], gyro[2],
                                                                                 mag[0], mag[1], mag[2],
                                                                                 fusion.heading, fusion.pitch,
                                                                                 fusion.roll
                                                                                 ))
        except OSError as e:
            print('Failed to read sensors {}'.format(e))
        time.sleep(0.1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--tcp-server-port", help="TCP port for incoming connections", default=2323, required=True)
    parser.add_argument("--log-dir", help="Directory to store logs",  required=False)

    main(parser.parse_args())
