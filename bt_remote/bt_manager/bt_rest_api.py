import os

from bt_device import BtRemoteFunction
from sys import platform
if platform != "darwin":
    from bt_manager import BtManager

BT_CONF_DIR = 'BT_CONF_DIR'

FUNC_STR_TO_ENUM = {
    'route': BtRemoteFunction.ROUTE,
    'timer': BtRemoteFunction.TIMER,
    'autopilot': BtRemoteFunction.AUTOPILOT,
    'unassigned': BtRemoteFunction.NONE
}

FUNC_ENUM_TO_STR = {
    BtRemoteFunction.ROUTE: 'route',
    BtRemoteFunction.TIMER: 'timer',
    BtRemoteFunction.AUTOPILOT: 'autopilot',
    BtRemoteFunction.NONE: 'unassigned'
}


def get_bt_devices():
    bt_manager = BtManager.load_instance(os.getenv(BT_CONF_DIR))
    devices_list = bt_manager.get_cached_devices_list()
    bt_devices = bt_devices_to_json(devices_list)

    return bt_devices


def bt_devices_to_json(devices_list):
    bt_devices = []
    for d in devices_list:
        bt_devices.append({
            'bd_addr': d.addr,
            'name': d.name,
            'is_paired': d.paired,
            'is_connected': d.connected,
            'function': FUNC_ENUM_TO_STR[d.function],
        })
    return bt_devices


def pair_bt_device(body=None):
    bd_addr = body['bd_addr']
    function = body['function']
    if function not in FUNC_STR_TO_ENUM:
        return {'not supported': 420}

    bt_manager = BtManager.load_instance(os.getenv(BT_CONF_DIR))

    bt_manager.pair_device(bd_addr, FUNC_STR_TO_ENUM[function])
    for d in bt_manager.get_cached_devices_list():
        if d.addr == bd_addr:
            if d.paired:
                return {'paired': 200}

    return {'failed': 420}


def unpair_bt_device(bd_addr):
    bt_manager = BtManager.load_instance(os.getenv(BT_CONF_DIR))

    bt_manager.remove_device(bd_addr)
    for d in bt_manager.get_cached_devices_list():
        if d.addr == bd_addr:
            if d.paired:
                return {'still paired': 420}

    return {'removed': 200}


def connect_bt_device(bd_addr):
    bt_manager = BtManager.load_instance(os.getenv(BT_CONF_DIR))

    bt_manager.connect_device(bd_addr)
    for d in bt_manager.get_cached_devices_list():
        if d.addr == bd_addr:
            if d.connected:
                return {'still connected': 420}

    return {'removed': 200}


def get_bt_scan_result():
    bt_manager = BtManager.load_instance(os.getenv(BT_CONF_DIR))

    if bt_manager.is_busy():
        return {
            'in_progress': True,
            'devices': []
        }
    else:
        devices_list = bt_manager.get_scanned_devices()
        bt_devices = bt_devices_to_json(devices_list)
        return {
            'in_progress': False,
            'devices': bt_devices
        }


def start_scan():
    bt_manager = BtManager.load_instance(os.getenv(BT_CONF_DIR))
    bt_manager.perform_scan()
    return {'removed': 200}
