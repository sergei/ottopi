import argparse
import time

from bt_device import BtRemoteFunction
from bt_manager import BtManager
from rest_client_bang_acp import BangAcpRestClient
from bt_remote_hog import HogBtRemote
from rest_client_routes import RoutesRestClient
from rest_client_start_timer import StartTimerRestClient


def get_device_map(bt_manager):
    device_map = {}
    devices = bt_manager.get_cached_devices_list()
    for device in devices:
        device_map[device.addr] = device.function

    print(device_map)
    return device_map


def main(args):
    bt_manager = BtManager.load_instance(args.data_dir)

    clients = {}
    old_dev_addr_set = set()
    old_bt_dev_map = {}

    while True:
        new_bt_dev_map = get_device_map(bt_manager)
        new_dev_addr_set = set(new_bt_dev_map.keys())
        print('New devices {}'.format(new_dev_addr_set))
        print('Old devices {}'.format(old_dev_addr_set))
        if old_dev_addr_set != new_dev_addr_set:
            # Determine what devices to add and what to remove
            removed_addr_set = old_dev_addr_set - new_dev_addr_set
            added_addr_set = new_dev_addr_set - old_dev_addr_set
            old_dev_addr_set = new_dev_addr_set

            # Stop removed clients
            for bt_addr in removed_addr_set:
                print('Device {} was removed'.format(bt_addr))
                clients[bt_addr].stop()
                print('Client for device {} was stopped'.format(bt_addr))

            # Start newly added BT devices
            for bt_addr in added_addr_set:
                bt_remote_func = new_bt_dev_map[bt_addr]
                print('Device {} {} was added'.format(bt_addr, bt_remote_func))

                rest_client = find_event_handler(bt_remote_func)

                if rest_client is not None:
                    clients[bt_addr] = HogBtRemote(bt_manager, bt_addr, rest_client.on_remote_key)
                    print('Starting polling device {} for {} control'.format(bt_addr, bt_remote_func))
                    clients[bt_addr].start_polling()

        # Check if the function of old BT device has changed
        for bt_addr in old_dev_addr_set:
            if bt_addr in old_bt_dev_map and new_bt_dev_map[bt_addr] != old_bt_dev_map[bt_addr]:
                bt_remote_func = new_bt_dev_map[bt_addr]
                print('Device {} function changed from {} to {} '.format(bt_addr, old_bt_dev_map[bt_addr],
                                                                         bt_remote_func))
                rest_client = find_event_handler(bt_remote_func)
                if rest_client is not None:
                    if bt_addr in clients:
                        print('Switching  device {} to {} control'.format(bt_addr, bt_remote_func))
                        clients[bt_addr].set_event_handler(rest_client.on_remote_key)
                    else:
                        print('Starting polling device {} for {} control'.format(bt_addr, bt_remote_func))
                        clients[bt_addr].start_polling()

            old_bt_dev_map = new_bt_dev_map

        # Sleep before reading config again
        time.sleep(10)


def find_event_handler(bt_remote_func):
    rest_client = None
    if bt_remote_func == BtRemoteFunction.ROUTE:
        rest_client = RoutesRestClient()
    elif bt_remote_func == BtRemoteFunction.AUTOPILOT:
        rest_client = BangAcpRestClient()
    elif bt_remote_func == BtRemoteFunction.TIMER:
        rest_client = StartTimerRestClient()
    else:
        print('Unsupported BT remote function {}'.format(bt_remote_func))
    return rest_client


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--data-dir", help="Directory to keep GPX data",  required=True)

    main(parser.parse_args())
