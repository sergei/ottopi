#!/usr/bin/python

# noinspection PyUnresolvedReferences
from gi.repository import GObject

import dbus
import dbus.service
import dbus.mainloop.glib
from optparse import OptionParser
import bluezutils

BUS_NAME = 'org.bluez'
AGENT_INTERFACE = 'org.bluez.Agent1'
AGENT_PATH = "/test/agent"


class Rejected(dbus.DBusException):
    _dbus_error_name = "org.bluez.Error.Rejected"


class Agent(dbus.service.Object):
    def __init__(self, bus, path, dev_manager):
        super().__init__(bus, path)
        self.exit_on_release = True
        self.dev_manager = dev_manager

    def set_exit_on_release(self, exit_on_release):
        self.exit_on_release = exit_on_release

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="", out_signature="")
    def Release(self):
        print("Release")
        if self.exit_on_release:
            self.dev_manager.mainloop.quit()

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="os", out_signature="")
    def AuthorizeService(self, device, uuid):
        print("AuthorizeService (%s, %s)" % (device, uuid))
        authorize = self.dev_manager.ask("Authorize connection (yes/no): ")
        if authorize == "yes":
            return
        raise Rejected("Connection rejected by user")

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="o", out_signature="s")
    def RequestPinCode(self, device):
        print("RequestPinCode (%s)" % device)
        self.dev_manager.set_trusted(device)
        return self.dev_manager.ask("Enter PIN Code: ")

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="o", out_signature="u")
    def RequestPasskey(self, device):
        print("RequestPasskey (%s)" % device)
        self.dev_manager.set_trusted(device)
        passkey = self.dev_manager.ask("Enter passkey: ")
        return dbus.UInt32(passkey)

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="ouq", out_signature="")
    def DisplayPasskey(self, device, passkey, entered):
        print("DisplayPasskey (%s, %06u entered %u)" %
              (device, passkey, entered))

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="os", out_signature="")
    def DisplayPinCode(self, device, pincode):
        print("DisplayPinCode (%s, %s)" % (device, pincode))

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="ou", out_signature="")
    def RequestConfirmation(self, device, passkey):
        print("RequestConfirmation (%s, %06d)" % (device, passkey))
        confirm = self.dev_manager.ask("Confirm passkey (yes/no): ")
        if confirm == "yes":
            self.dev_manager.set_trusted(device)
            return
        raise Rejected("Passkey doesn't match")

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="o", out_signature="")
    def RequestAuthorization(self, device):
        print("RequestAuthorization (%s)" % device)
        auth = self.dev_manager.ask("Authorize? (yes/no): ")
        if auth == "yes":
            return
        raise Rejected("Pairing rejected")

    @dbus.service.method(AGENT_INTERFACE,
                         in_signature="", out_signature="")
    def Cancel(self):
        print("Cancel")


class DeviceManager:
    def __init__(self):
        dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
        self.mainloop = GObject.MainLoop()
        self.bus = dbus.SystemBus()
        self.device = None
        self.dev_path = None

    @staticmethod
    def ask(prompt):
        return input(prompt)

    def set_trusted(self, path):
        props = dbus.Interface(self.bus.get_object("org.bluez", path),
                               "org.freedesktop.DBus.Properties")
        props.Set("org.bluez.Device1", "Trusted", True)

    def dev_connect(self, path):
        dev = dbus.Interface(self.bus.get_object("org.bluez", path),
                             "org.bluez.Device1")
        dev.Connect()

    def pair_reply(self):
        print("Device paired")
        self.set_trusted(self.dev_path)
        self.dev_connect(self.dev_path)
        self.mainloop.quit()

    def pair_error(self, error):
        err_name = error.get_dbus_name()
        if err_name == "org.freedesktop.DBus.Error.NoReply" and self.device is not None:
            print("Timed out. Cancelling pairing")
            self.device.CancelPairing()
        else:
            print("Creating device failed: %s" % error)

        self.mainloop.quit()

    def pair(self, bt_addr, cap):
        agent_path = AGENT_PATH
        agent = Agent(self.bus, agent_path, self)
        obj = self.bus.get_object(BUS_NAME, "/org/bluez")
        manager = dbus.Interface(obj, "org.bluez.AgentManager1")
        manager.RegisterAgent(agent_path, cap)

        print("Agent registered")

        self.device = bluezutils.find_device(bt_addr, options.adapter_pattern)
        self.dev_path = self.device.object_path
        agent.set_exit_on_release(False)

        self.device.Pair(reply_handler=self.pair_reply, error_handler=self.pair_error, timeout=60000)

        self.mainloop.run()

        # adapter.UnregisterAgent(path)
        # print("Agent unregistered")


if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("-i", "--adapter", action="store",
                      type="string",
                      dest="adapter_pattern",
                      default=None)
    parser.add_option("-c", "--capability", action="store",
                      type="string", dest="capability")
    parser.add_option("-t", "--timeout", action="store",
                      type="int", dest="timeout",
                      default=60000)
    (options, args) = parser.parse_args()

    capability = "KeyboardDisplay"
    if options.capability:
        capability = options.capability

    if len(args) > 0:
        device_manager = DeviceManager()
        device_manager.pair(args[0], capability)
