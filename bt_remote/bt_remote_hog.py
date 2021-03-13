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
        ecodes.KEY_VOLUMEDOWN: BtRemote.MINUS_BUTTON,  # Tunai and Satechi buttons
        ecodes.KEY_VOLUMEUP: BtRemote.PLUS_BUTTON,
        ecodes.KEY_PREVIOUSSONG: BtRemote.PREV_BUTTON,
        ecodes.KEY_NEXTSONG: BtRemote.NEXT_BUTTON,
        ecodes.KEY_PLAYPAUSE: BtRemote.PLAY_BUTTON,

        ecodes.KEY_HOMEPAGE: BtRemote.VENDOR_BUTTON,  # Tunai button

        # 8BitDo mini controller in keyboard mode
        ecodes.KEY_D: BtRemote.MINUS_BUTTON,  # D-pad Down
        ecodes.KEY_C: BtRemote.PLUS_BUTTON,   # D-pad Up
        ecodes.KEY_E: BtRemote.PREV_BUTTON,   # D-pad left
        ecodes.KEY_F: BtRemote.NEXT_BUTTON,   # D-pad right
        ecodes.KEY_O: BtRemote.PLAY_BUTTON,   # start button
        ecodes.KEY_N: BtRemote.VENDOR_BUTTON,  # select button
        ecodes.KEY_K: BtRemote.VENDOR_BUTTON,  # L button
        ecodes.KEY_M: BtRemote.VENDOR_BUTTON,  # R button
        ecodes.KEY_H: BtRemote.VENDOR_BUTTON,  # X button
        ecodes.KEY_J: BtRemote.VENDOR_BUTTON,  # B button
        ecodes.KEY_I: BtRemote.VENDOR_BUTTON,  # Y button
        ecodes.KEY_G: BtRemote.VENDOR_BUTTON,  # A button

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
            print('Trying to connect to {}'.format(self.addr))
            self.bt_manager.connect_device(self.addr)
            devices = [evdev.InputDevice(path) for path in evdev.list_devices()]
            input_devices = []
            for d in devices:
                if d.uniq.lower() == self.addr.lower():
                    input_devices.append(d)

            if len(input_devices) == 0:
                print('No devices found for address {}, will try again'.format(self.addr))
                time.sleep(1)
                continue

            # We might have multiple devices for the same address, so read from all of them
            selector = selectors.DefaultSelector()
            for device in input_devices:
                # This works because InputDevice has a `fileno()` method.
                selector.register(device, selectors.EVENT_READ)
                print('Registered {}'.format(device.path))

            while self.keep_running:
                for key, mask in selector.select(timeout=2):
                    device = key.fileobj
                    try:
                        # noinspection PyUnresolvedReferences
                        for event in device.read():
                            if event.type == evdev.ecodes.EV_KEY:
                                print(evdev.categorize(event))
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
