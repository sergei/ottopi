from rest_client import RestClient


class RestClientBtMgr(RestClient):
    """
    Use this class to map BT remote control keys to ottopi  B&G autopilot REST APIs
    """
    def __init__(self):
        super().__init__()

    def connect_to(self, bd_addr):
        print('Connecting to device {}'.format(bd_addr))
        self.post('bluetooth/connect/{bd_addr}'.format(bd_addr=bd_addr), {})

    def get_device_map(self) -> dict:
        dev_map = {}
        devices = self.get('/bluetooth/devices')
        for device in devices:
            if device['is_paired'] and device['function'] != 'none':
                dev_map[device['bd_addr']] = device['function']

        return dev_map
