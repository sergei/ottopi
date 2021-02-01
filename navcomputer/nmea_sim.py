import argparse
import selectors
import socket

sel = selectors.DefaultSelector()
connections = []


def accept(sock, mask):
    conn, addr = sock.accept()  # Should be ready
    print('accepted', conn, 'from', addr)
    conn.setblocking(False)
    conn.send(bytes('Hello', 'utf-8'))
    sel.register(conn, selectors.EVENT_READ, read)
    connections.append(conn)


def read(conn, mask):
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


def nmea_sim(args):
    with open(args.nmea_file, 'rb') as nmea_file:
        sock = socket.socket()
        sock.bind(('localhost', args.tcp_port))
        sock.listen(100)
        sock.setblocking(False)
        sel.register(sock, selectors.EVENT_READ, accept)
        print('Listening on port {}'.format(args.tcp_port))
        while True:
            events = sel.select(timeout=1)
            for key, mask in events:
                callback = key.data
                callback(key.fileobj, mask)

            # Read and send NMEA epoch
            for line in nmea_file:
                chunk_size = 10
                for i in range(0, len(line), chunk_size):
                    chunk_end = i + chunk_size
                    if chunk_end > len(line):
                        chunk_end = len(line)
                    for conn in connections:
                        conn.send(line[i:chunk_end])
                if b'$GPRMC' in line:
                    break


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Utility to playback NMEA file", fromfile_prefix_chars='@')
    parser.add_argument("--nmea-file", help="NMEA file", required=True)
    parser.add_argument("--tcp-port", help="TCP port", default=12345, required=False)
    nmea_sim(parser.parse_args())
