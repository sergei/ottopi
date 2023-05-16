import argparse
import glob
import gzip
import os
import socket  # for UDP broadcast
from datetime import datetime
from time import sleep


def main(args):
    log_list = []
    log_list += sorted(glob.glob(args.replay_dir + os.sep + '*-log.txt'))

    # Create broadcast UPD socket on port 2020

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    prev_msg_time_stamp = None
    for log in log_list:
        if log.endswith('.gz'):
            f = gzip.open(log, 'rt')
        else:
            f = open(log, 'rt')
        print('Replaying {}'.format(log))
        for line in f:
            if line.find('RAW_N2K,') > 0:
                # Date and time are the first two fields separated by a space
                date = line.split(' ')[0]
                time = line.split(' ')[1]
                dt = datetime.strptime(date + ' ' + time, '%Y-%m-%d %H:%M:%S.%f')
                # Convert to Unix time
                msg_time_stamp = dt.timestamp()

                # Time to sleep before sending this message
                if prev_msg_time_stamp is not None:
                    sleep_time = msg_time_stamp - prev_msg_time_stamp
                    print('Sleeping for {:.3f} seconds'.format(sleep_time))
                    sleep(sleep_time)
                prev_msg_time_stamp = msg_time_stamp

                idx = line.find(' R ')
                if idx > 0:
                    can_line = line[idx + 3:-2]
                    print('Sending: {}'.format(can_line))
                    s.sendto(can_line.encode(), ('<broadcast>', args.udp_port))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--udp-port", help="UDP port to broadcast data", default=2024)
    parser.add_argument("--replay-dir", help="Replay logs found in this directory", required=True)
    main(parser.parse_args())
