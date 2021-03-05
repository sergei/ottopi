import threading
import time

from typing import List

# noinspection PyUnresolvedReferences
from gi.repository import GLib

import dbus
import dbus.mainloop.glib
import bluezutils
from bt_device import BtDevFromProperties, BtDevice


class BtScanner:
    bt_dev_list: List[BtDevice]

    def __init__(self):
        self.t = None
        self.mainloop = None
        self.devices = {}
        self.is_running = False
        self.bt_dev_list = []

    def scan(self, timeout=10000):
        self.t = threading.Thread(target=self.__scan, name='bt_scan_thread', args=[timeout])
        self.t.start()
        self.is_running = True

    def is_busy(self):
        if self.t is None:
            return False

        if not self.t.is_alive() and not self.is_running:
            self.t.join()
            return False
        else:
            return True

    def stop_timer(self):
        print('Scan timer expired')
        self.mainloop.quit()
        return True

    def interfaces_added(self, path, interfaces):
        properties = interfaces["org.bluez.Device1"]
        if not properties:
            return

        print('Interface Added {}'.format(path))
        if path in self.devices:
            self.devices[path].update(properties)
        else:
            self.devices[path] = properties

    # noinspection PyUnusedLocal
    def properties_changed(self, interface, changed, invalidated, path):
        if interface != "org.bluez.Device1":
            return

        print('Properties Changed {}'.format(path))
        if path in self.devices:
            self.devices[path].update(changed)
        else:
            self.devices[path] = changed

    def property_changed(self, name, value):
        print('Property Changed {}'.format(name))
        if name == "Discovering" and not value:
            self.mainloop.quit()

    def __scan(self, timeout):
        dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
        bus = dbus.SystemBus()
        bus.add_signal_receiver(self.interfaces_added,
                                dbus_interface="org.freedesktop.DBus.ObjectManager",
                                signal_name="InterfacesAdded")

        bus.add_signal_receiver(self.properties_changed,
                                dbus_interface="org.freedesktop.DBus.Properties",
                                signal_name="PropertiesChanged",
                                arg0="org.bluez.Device1",
                                path_keyword="path")

        bus.add_signal_receiver(self.property_changed,
                                dbus_interface="org.bluez.Adapter1",
                                signal_name="PropertyChanged")

        om = dbus.Interface(bus.get_object("org.bluez", "/"),
                            "org.freedesktop.DBus.ObjectManager")
        objects = om.GetManagedObjects()
        for path, interfaces in objects.items():
            if "org.bluez.Device1" in interfaces:
                self.devices[path] = interfaces["org.bluez.Device1"]

        adapter = bluezutils.find_adapter()
        try:
            print('StartDiscovery()')
            adapter.StartDiscovery()

            self.mainloop = GLib.MainLoop()
            GLib.timeout_add(timeout, self.stop_timer)
            self.mainloop.run()
        except (dbus.exceptions.DBusException, LookupError) as e:
            print(e)

        self.is_running = False

        self.bt_dev_list.clear()
        for path in self.devices:
            props = self.devices[path]
            bt_device = BtDevFromProperties(props)
            self.bt_dev_list.append(bt_device)

        try:
            print('StopDiscovery()')
            adapter.StopDiscovery()
        except (dbus.exceptions.DBusException, LookupError) as e:
            print(e)


def print_device(addr, properties):
    print("[ " + addr + " ]")

    for key in properties.keys():
        value = properties[key]
        if type(value) is dbus.String:
            value = str(value).encode('ascii', 'replace')
        if key == "Class":
            print("    %s = 0x%06x" % (key, value))
        else:
            print("    %s = %s" % (key, value))

    print()

    properties["Logged"] = True


if __name__ == '__main__':
    bt_scanner = BtScanner()
    bt_scanner.scan()
    print('Scan started')
    while bt_scanner.is_busy():
        print('Waiting ...')
        time.sleep(1)

    for p in bt_scanner.devices:
        d = bt_scanner.devices[p]
        if "Address" in d:
            address = d["Address"]
            print_device(address, d)

    for d in bt_scanner.bt_dev_list:
        print(d)
