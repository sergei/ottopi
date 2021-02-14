class BtController:
    MINUS_BUTTON = 1
    PLUS_BUTTON = 2
    PREV_BUTTON = 3
    NEXT_BUTTON = 4
    PLAY_BUTTON = 5
    VENDOR_BUTTON = 6

    def __init__(self, addr):
        self.addr = addr

    def connect(self):
        pass

    def poll_device(self, event_handler):
        pass
