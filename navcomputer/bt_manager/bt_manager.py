import sys
import time
from optparse import OptionParser
from typing import List

import dbus

from bt_agent import DeviceManager
from bt_device import BtDevFromProperties, BtDevice
from bt_scanner import BtScanner


class BtManager:
    bt_scanner: BtScanner
    bt_scanner = BtScanner()

    def __init__(self):
        pass

    def perform_scan(self):
        self.bt_scanner.scan()

    def get_scanned_devices(self) -> List[BtDevice]:
        return self.bt_scanner.bt_dev_list

    @staticmethod
    def get_cached_devices_list() -> List[BtDevice]:
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

        return bt_dev_list

    @staticmethod
    def pair_device(bt_addr: str):
        dev_manager = DeviceManager()
        dev_manager.pair(bt_addr, 'KeyboardDisplay')

    @staticmethod
    def remove_device(bt_addr: str):
        dev_manager = DeviceManager()
        dev_manager.remove(bt_addr)

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
            bt_manager.pair_device(args[1])
        else:
            print('Please specify address')
    elif cmd == 'remove':
        if len(args) > 1:
            bt_manager.remove_device(args[1])
        else:
            print('Please specify address')
    else:
        print('Unknown command')
