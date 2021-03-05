import time
from typing import List

import dbus

from bt_device import BtDevFromProperties, BtDevice
from bt_scanner import BtScanner


class BtManager:
    bt_scanner: BtScanner

    def __init__(self):
        self.bt_scanner = BtScanner()

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

    def pair_device(self, bt_addr: str):
        pass

    def remove_device(self, bt_addr: str):
        pass

    def is_busy(self):
        return self.bt_scanner.is_busy()


if __name__ == '__main__':
    bt_manager = BtManager()

    for d in bt_manager.get_cached_devices_list():
        print(d)

    bt_manager.perform_scan()
    while bt_manager.is_busy():
        time.sleep(1)

    for d in bt_manager.get_scanned_devices():
        print(d)
