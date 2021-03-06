from bt_remote import BtRemote
from rest_client import RestClient


class BangAcpRestClient(RestClient):
    """
    Use this class to map BT remote control keys to ottopi  B&G autopilot REST APIs
    """
    def __init__(self):
        super().__init__()

    def on_remote_key(self, key):
        if key == BtRemote.PLAY_BUTTON:
            self.post('autopilot/tack', {})
        elif key == BtRemote.NEXT_BUTTON:
            self.post('autopilot/steer/10', {})
        elif key == BtRemote.PREV_BUTTON:
            self.post('autopilot/steer/-10', {})
        elif key == BtRemote.PLUS_BUTTON:
            self.post('autopilot/steer/2', {})
        elif key == BtRemote.MINUS_BUTTON:
            self.post('autopilot/steer/-2', {})
