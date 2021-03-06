import json
import sys
import time
from optparse import OptionParser
from typing import List

import dbus

from bt_agent import DeviceManager
from bt_device import BtDevFromProperties, BtDevice, BtRemoteFunction
from bt_scanner import BtScanner


class BtManager:
    bt_scanner: BtScanner
    bt_scanner = BtScanner()

    def __init__(self, conf_name: str = None):
        self.conf_name = conf_name
        self.dev_func_map = self.load_bt_dev_map(conf_name)

    @staticmethod
    def load_bt_dev_map(conf_name):
        dev_map = {}
        if conf_name is not None:
            try:
                with open(conf_name, 'r') as f:
                    dev_map = json.load(f)
            except Exception as e:
                print(e)
        return dev_map

    def perform_scan(self):
        self.bt_scanner.scan()

    def fill_dev_functions(self, dev_list: List[BtDevice]):
        for dev in dev_list:
            if dev.addr in self.dev_func_map:
                dev.function = self.dev_func_map[dev.addr]

    def get_scanned_devices(self) -> List[BtDevice]:
        self.fill_dev_functions(self.bt_scanner.bt_dev_list)
        return self.bt_scanner.bt_dev_list

    def get_cached_devices_list(self) -> List[BtDevice]:
        bt_dev_list = []
        dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
        bus = dbus.SystemBus()

        manager = dbus.Interface(bus.get_object("org.bluez", "/"),
                                 "org.freedesktop.DBus.ObjectManager")
        objects = manager.GetManagedObjects()
        all_devices = [str(path) for path, interfaces in objects.items() if
                       "org.bluez.Device1" in interfaces.keys()]

        for path, interfaces in objects.items():
            if "org.bluez.Adapter1" not in interfaces.keys():
                continue
            device_list = [x for x in all_devices if x.startswith(path + "/")]
            for dev_path in device_list:
                dev = objects[dev_path]
                properties = dev["org.bluez.Device1"]
                bt_device = BtDevFromProperties(properties)
                bt_dev_list.append(bt_device)

        self.fill_dev_functions(bt_dev_list)
        return bt_dev_list

    def pair_device(self, bt_addr: str, function: BtRemoteFunction):
        dev_manager = DeviceManager()
        dev_manager.pair(bt_addr, 'KeyboardDisplay')
        self.dev_func_map[bt_addr] = function
        self.update_conf_file()

    def remove_device(self, bt_addr: str):
        dev_manager = DeviceManager()
        dev_manager.remove(bt_addr)
        self.dev_func_map.pop(bt_addr)
        self.update_conf_file()

    def update_conf_file(self):
        if self.conf_name is not None:
            try:
                with open(self.conf_name, 'w') as f:
                    print('Updating {}'.format(self.conf_name))
                    json.dump(self.dev_func_map, f)
            except Exception as e:
                print(e)

    @staticmethod
    def connect_device(bt_addr: str):
        dev_manager = DeviceManager()
        dev_manager.connect(bt_addr)

    @staticmethod
    def disconnect_device(bt_addr: str):
        dev_manager = DeviceManager()
        dev_manager.disconnect(bt_addr)

    def is_busy(self):
        return self.bt_scanner.is_busy()


if __name__ == '__main__':
    parser = OptionParser()
    (options, args) = parser.parse_args()

    if len(args) == 0:
        print('Please specify the command')
        sys.exit(0)

    bt_manager = BtManager()

    cmd = args[0]
    if cmd == 'devices':
        for d in bt_manager.get_cached_devices_list():
            print(d)
    elif cmd == 'scan':
        bt_manager.perform_scan()
        while bt_manager.is_busy():
            time.sleep(1)
        for d in bt_manager.get_scanned_devices():
            print(d)
    elif cmd == 'connect':
        if len(args) > 1:
            bt_manager.connect_device(args[1])
        else:
            print('Please specify address')
    elif cmd == 'disconnect':
        if len(args) > 1:
            bt_manager.disconnect_device(args[1])
        else:
            print('Please specify address')
    elif cmd == 'pair':
        if len(args) > 1:
            bt_manager.pair_device(args[1], BtRemoteFunction.ROUTE)
        else:
            print('Please specify address')
    elif cmd == 'remove':
        if len(args) > 1:
            bt_manager.remove_device(args[1])
        else:
            print('Please specify address')
    else:
        print('Unknown command')
