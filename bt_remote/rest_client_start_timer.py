from bt_remote import BtRemote
from rest_client import RestClient


class StartTimerRestClient(RestClient):
    """
    Use this class to map BT remote control keys to ottopi  Start timer REST APIs
    """
    def __init__(self):
        super().__init__()

    def on_remote_key(self, key):
        if key == BtRemote.PLAY_BUTTON:
            self.post('timer/start', {})
        elif key == BtRemote.VENDOR_BUTTON:
            self.post('timer/stop', {})
        elif key == BtRemote.PREV_BUTTON:
            self.post('timer/sync', {})
