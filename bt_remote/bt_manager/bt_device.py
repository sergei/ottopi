from enum import Enum


class BtRemoteFunction(str, Enum):
    NONE = 'none'
    ROUTE = 'route'
    TIMER = 'timer'
    AUTOPILOT = 'autopilot'


class BtDevice:

    def __init__(self, addr: str, name: str, paired: bool, trusted: bool, connected: bool):
        self.addr = addr
        self.name = name
        self.paired = paired
        self.trusted = trusted
        self.connected = connected
        self.function = BtRemoteFunction.NONE

    def __str__(self):
        connected = 'connected' if self.connected else ''
        paired = 'paired ' if self.paired else ''
        return self.addr + ' ' + self.name + ' ' + paired + ' ' + connected


# noinspection PyPep8Naming
def BtDevFromProperties(props):
    addr = props["Address"] if "Address" in props else "Unknown"
    name = props["Name"] if "Name" in props else addr
    paired = props["Paired"] if "Paired" in props else False
    trusted = props["Trusted"] if "Trusted" in props else False
    connected = props["Connected"] if "Connected" in props else False
    return BtDevice(addr, name, paired, trusted, connected)
