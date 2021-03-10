import selectors
import threading
import time

# noinspection PyUnresolvedReferences
import evdev
# noinspection PyUnresolvedReferences
from evdev import ecodes

from bt_manager import BtManager
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

    t: threading.Thread
    keep_running: bool
    bt_manager: BtManager

    def __init__(self, bt_manager: BtManager, addr: str, event_handler):
        super().__init__(addr)
        self.t = threading.Thread(target=self.__poll_device, name='bt_remote_hog', args=[])
        self.keep_running = True
        self.event_handler = event_handler
        self.bt_manager = bt_manager

    def start_polling(self):
        self.t.start()

    def set_event_handler(self, event_handler):
        self.event_handler = event_handler

    def __poll_device(self):

        while self.keep_running:
            event_handler = self.event_handler
            self.bt_manager.connect_device(self.addr)
            devices = [evdev.InputDevice(path) for path in evdev.list_devices()]
            input_devices = []
            for d in devices:
                if d.uniq.lower() == self.addr.lower():
                    input_devices.append(d)

            if len(input_devices) == 0:
                print('No devices found for address {}, trying again'.format(self.addr))
                time.sleep(10)

            # We might have multiple devices for the same address, so read from all of them
            selector = selectors.DefaultSelector()
            for device in input_devices:
                # This works because InputDevice has a `fileno()` method.
                selector.register(device, selectors.EVENT_READ)
                print('Registered {}'.format(device.path))

            while True:
                for key, mask in selector.select():
                    device = key.fileobj
                    try:
                        # noinspection PyUnresolvedReferences
                        for event in device.read():
                            print(evdev.categorize(event))
                            if event.type == evdev.ecodes.EV_KEY:
                                if event.value == 1 and event.code in self.BUTTONS_MAP:
                                    bt_event = self.BUTTONS_MAP.get(event.code)
                                    try:
                                        event_handler(bt_event)
                                    except Exception as e:
                                        print('Failed to handle event {}:'.format(e))
                    except OSError as e:
                        print('Event reading error :{}'.format(e))
                        time.sleep(1)
                        continue

    def stop(self):
        self.keep_running = False
        self.t.join(2)
