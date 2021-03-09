import threading
import time
from urllib.parse import quote

# noinspection PyUnresolvedReferences
import evdev
# noinspection PyUnresolvedReferences
from evdev import ecodes

from bt_remote import BtRemote
from rest_client import RestClient


class HogBtRemote(BtRemote):
    """
    This controller is for BT remotes implementing HoG ( HID over GATT)
    https://stackoverflow.com/questions/54745576/detecting-the-buttons-on-a-bluetooth-remote-hid-over-gatt
    """

    BUTTONS_MAP = {
        ecodes.KEY_VOLUMEDOWN: BtRemote.MINUS_BUTTON,
        ecodes.KEY_VOLUMEUP: BtRemote.PLUS_BUTTON,
        ecodes.KEY_PREVIOUSSONG: BtRemote.PREV_BUTTON,
        ecodes.KEY_NEXTSONG: BtRemote.NEXT_BUTTON,
        ecodes.KEY_PLAYPAUSE: BtRemote.PLAY_BUTTON,
        ecodes.KEY_HOMEPAGE: BtRemote.VENDOR_BUTTON,
    }

    t: threading.Thread
    keep_running: bool

    def __init__(self, addr: str, event_handler):
        super().__init__(addr)
        self.t = threading.Thread(target=self.__poll_device, name='bt_remote_hog', args=[event_handler])
        self.keep_running = True

    def start_polling(self):
        self.t.start()

    def __poll_device(self, event_handler):
        bt_manager_client = RestClient()

        while self.keep_running:
            print('Connecting to device {}'.format(self.addr))
            bt_manager_client.post('bluetooth/connect/{bd_addr}'.format(bd_addr=quote(self.addr)), {})
            print('Looking for event device')
            devices = [evdev.InputDevice(path) for path in evdev.list_devices()]
            input_device = None
            for d in devices:
                if d.uniq.lower() == self.addr.lower():
                    input_device = d
                    break

            if input_device is None:
                print('Device is not found, trying again')
                time.sleep(10)
                continue

            device = evdev.InputDevice(input_device.path)
            print('Opened {}'.format(device))
            try:
                for event in device.read_loop():
                    print(evdev.categorize(event))
                    if event.type == evdev.ecodes.EV_KEY:
                        if event.value == 1 and event.code in self.BUTTONS_MAP:
                            bt_event = self.BUTTONS_MAP.get(event.code)
                            event_handler(bt_event)
            except OSError:
                time.sleep(1)
                continue

    def stop(self):
        self.keep_running = False
        self.t.join(2)
