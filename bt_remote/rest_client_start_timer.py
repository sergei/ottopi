from bt_remote import BtRemote
from rest_client import RestClient


class StartTimerRestClient(RestClient):
    """
    Use this class to map BT remote control keys to ottopi  Start timer REST APIs
    """
    def __init__(self, url=None):
        super().__init__(url)

    def on_remote_key(self, key):
        if key == BtRemote.PLAY_BUTTON:
            self.post('timer/start', {})
        elif key == BtRemote.VENDOR_BUTTON:
            self.post('timer/say_state', {})
        elif key == BtRemote.PREV_BUTTON:
            self.post('timer/sync', {})
        elif key == BtRemote.NEXT_BUTTON:
            self.post('timer/sync', {})
        elif key == BtRemote.MINUS_BUTTON:
            self.post('timer/stop', {})
        elif key == BtRemote.PLUS_BUTTON:
            self.post('timer/say_state', {})


if __name__ == '__main__':
    from sim_bt_remote import sim_bt_remote
    sim_bt_remote(StartTimerRestClient('http://localhost:5555/'))
