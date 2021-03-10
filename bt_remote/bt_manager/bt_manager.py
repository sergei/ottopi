import json
import os
import sys
import time
from optparse import OptionParser
from typing import List

import dbus

from bt_agent import DeviceManager
from bt_device import BtDevFromProperties, BtDevice, BtRemoteFunction
from bt_scanner import BtScanner

BT_CONF_NAME = 'bt_conf.json'


class BtManager:
    bt_scanner: BtScanner
    dev_manager: DeviceManager

    __instance = None

    @staticmethod
    def get_instance():
        """ Static access method """
        if BtManager.__instance is None:
            BtManager()
        return BtManager.__instance

    @staticmethod
    def load_instance(conf_dir):
        instance = BtManager.get_instance()
        instance.load_config(conf_dir)
        return instance

    def __init__(self):
        """ Virtually private constructor.  """
        if BtManager.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            BtManager.__instance = self
            self.conf_name = None
            self.dev_func_map = None
            self.bt_scanner = BtScanner()
            self.dev_manager = DeviceManager()

    def load_config(self, conf_dir: str = None):
        self.conf_name = os.path.expanduser(conf_dir + os.sep + BT_CONF_NAME)
        self.dev_func_map = self.load_bt_dev_map(self.conf_name)

    def reread_config(self):
        self.dev_func_map = self.load_bt_dev_map(self.conf_name)

    def update_conf_file(self):
        if self.conf_name is not None:
            try:
                with open(self.conf_name, 'w') as f:
                    print('Updating {}'.format(self.conf_name))
                    json.dump(self.dev_func_map, f)
            except Exception as e:
                print(e)

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
        self.reread_config()
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

    def pair_device(self, bd_addr: str, function: BtRemoteFunction):
        # Check if device is already paired, then we just change the map settings
        if bd_addr not in self.dev_func_map:
            self.dev_manager.pair(bd_addr)

        self.dev_func_map[bd_addr] = function
        self.update_conf_file()

    def remove_device(self, bt_addr: str):
        self.dev_manager.remove(bt_addr)
        if bt_addr in self.dev_func_map:
            self.dev_func_map.pop(bt_addr)
        self.update_conf_file()

    def connect_device(self, bt_addr: str):
        self.dev_manager.connect(bt_addr)

    def disconnect_device(self, bt_addr: str):
        self.dev_manager.disconnect(bt_addr)

    def is_busy(self):
        return self.bt_scanner.is_busy()


if __name__ == '__main__':
    parser = OptionParser()
    (options, args) = parser.parse_args()

    if len(args) == 0:
        print('Please specify the command')
        sys.exit(0)

    bt_manager = BtManager.get_instance()

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
