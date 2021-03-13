import time
import threading

try:
    import RPi.GPIO as GPIO
except (ModuleNotFoundError, RuntimeError):
    print("B&G autopilot is not attached")


class BangControl:
    AP_ON = 1  # black-white
    AP_OFF = 2  # black-brown
    PLUS_TEN = 3  # black-yellow
    MINUS_TEN = 4  # black-blue
    PLUS_ONE = 5  # black-violet
    MINUS_ONE = 6  # black-green
    LED = 7  # red

    BUTTON_PUSH_DURATION_SEC = 0.5

    GPIO_MAP = {
        AP_ON: 35,
        AP_OFF: 33,
        PLUS_TEN: 40,
        MINUS_TEN: 36,
        PLUS_ONE: 37,
        MINUS_ONE: 32,
    }

    def __init__(self):
        try:
            import RPi.GPIO as GPIO
            # Initialize all GPIOs as outputs
            GPIO.setmode(GPIO.BOARD)
            GPIO.setup(list(BangControl.GPIO_MAP.values()), GPIO.OUT)
            GPIO.output(list(BangControl.GPIO_MAP.values()), GPIO.LOW)
            self.bang_connected = True
            self.bang_thread = None
        except (ModuleNotFoundError, RuntimeError):
            self.bang_connected = False
            print("B&G autopilot is not attached")

    def is_connected(self):
        return self.bang_connected

    def steer(self, degrees):
        if self.is_ready():
            self.bang_thread = threading.Thread(target=self.__steer, name='bang', args=[degrees])
            self.bang_thread.start()
            return True

        return False

    def tack(self):
        return self.push_buttons([BangControl.PLUS_TEN, BangControl.MINUS_TEN])

    def enable(self, enable):
        if enable:
            self.push_button(BangControl.AP_ON)
        else:
            self.push_button(BangControl.AP_OFF)

    def is_ready(self):
        if self.bang_thread is not None:
            print('Waiting for bang thread to finish')
            self.bang_thread.join(timeout=2.)
            if self.bang_thread.is_alive():
                print('bang thread got stuck')
                return False
        return True

    def push_button(self, button, n=1):
        buttons = [button]
        if self.is_ready():
            self.bang_thread = threading.Thread(target=self.__push_button, name='bang', args=[buttons, n])
            self.bang_thread.start()
            return True

        return False

    def push_buttons(self, buttons, n=1):
        if self.is_ready():
            self.bang_thread = threading.Thread(target=self.__push_button, name='bang', args=[buttons, n])
            self.bang_thread.start()

    def __steer(self, degrees):
        if degrees > 0:
            ten_button = BangControl.PLUS_TEN
            one_button = BangControl.PLUS_ONE
        else:
            ten_button = BangControl.MINUS_TEN
            one_button = BangControl.MINUS_ONE
        degrees = int(abs(degrees))
        deg_tens = int(degrees/10)
        deg_ones = degrees - deg_tens*10
        self.__push_button([ten_button], deg_tens)
        self.__push_button([one_button], deg_ones)

    @staticmethod
    def __push_button(buttons, n):
        gpios = []
        for button in buttons:
            gpios.append(BangControl.GPIO_MAP[button])

        for i in range(n):
            GPIO.output(gpios, GPIO.HIGH)
            time.sleep(BangControl.BUTTON_PUSH_DURATION_SEC)
            GPIO.output(gpios, GPIO.LOW)
            time.sleep(BangControl.BUTTON_PUSH_DURATION_SEC)
