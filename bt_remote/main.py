import argparse
import os
import sys
import time

import conf
from rest_client_bang_acp import BangAcpRestClient
from bt_device import BtRemoteFunction
from bt_manager import BtManager
from bt_remote_hog import HogBtRemote
from rest_client_routes import RoutesRestClient


def main(args):

    bt_conf_name = os.path.expanduser(args.data_dir + os.sep + conf.BT_CONF_NAME)
    clients = {}
    old_devices = set()

    while True:
        bt_dev_map = BtManager.load_bt_dev_map(bt_conf_name)
        new_devices = set(bt_dev_map.keys())
        print('New devices {}'.format(new_devices))
        print('Old devices {}'.format(old_devices))
        if old_devices != new_devices:
            # Determine what devices to add and what to remove
            removed_devices = old_devices - new_devices
            added_devices = new_devices - old_devices
            old_devices = new_devices

            # Stop removed clients
            for bt_addr in removed_devices:
                print('Device {} was removed'.format(bt_addr))
                clients[bt_addr].stop()
                print('Client for device {} was stopped'.format(bt_addr))

            for bt_addr in added_devices:
                bt_remote_func = bt_dev_map[bt_addr]
                print('Device {} {} was added'.format(bt_addr, bt_remote_func))
                rest_client = None
                if bt_remote_func == BtRemoteFunction.ROUTE:
                    rest_client = RoutesRestClient()
                elif bt_remote_func == BtRemoteFunction.AUTOPILOT:
                    rest_client = BangAcpRestClient()
                else:
                    print('Unsupported BT remote function {}'.format(bt_remote_func))

                if rest_client is not None:
                    clients[bt_addr] = HogBtRemote(bt_addr, rest_client.on_remote_key)
                    print('Starting polling device {} for {} control'.format(bt_addr, bt_remote_func))
                    clients[bt_addr].start_polling()

        # Sleep before reading config again
        time.sleep(10)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--data-dir", help="Directory to keep all data",  required=True)

    sys.path.append('../navcomputer/')
    main(parser.parse_args())
