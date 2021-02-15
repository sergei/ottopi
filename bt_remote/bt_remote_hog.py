# Ignore missing import in IDE
import threading
import time

# noinspection PyUnresolvedReferences
import evdev
# noinspection PyUnresolvedReferences
from evdev import ecodes

from bt_remote import BtRemote


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

    def __init__(self, addr):
        super().__init__(addr)

    def poll_device(self, event_handler):
        t = threading.Thread(target=self.__poll_device, name='flask_server', args=[event_handler])
        t.start()
        return t

    def __poll_device(self, event_handler):
        while True:
            try:
                print('Opening device {}'.format(self.addr))
                device = evdev.InputDevice(self.addr)
                print('Opened {}'.format(device))
                for event in device.read_loop():
                    if event.type == evdev.ecodes.EV_KEY:
                        print(evdev.categorize(event))
                        if event.value == 1 and event.code in self.BUTTONS_MAP:
                            bt_event = self.BUTTONS_MAP.get(event.code)
                            event_handler(bt_event)
            except FileNotFoundError:
                print('Probably not ready yet wait some ...')
                time.sleep(10)
