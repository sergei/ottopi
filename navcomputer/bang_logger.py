import time
from logger import Logger


class BangLogger:
    def __init__(self):
        self.buffer = ""

    def log(self, data):
        for b in data:
            if b == 0xFF:
                self.dump_log()

            if len(self.buffer) > 256:
                self.dump_log()

            self.buffer += '{:02x} '.format(b & 0x00FF)

    def dump_log(self):
        s = "B&G,{:.3f},".format(time.time()) + self.buffer + "\n"
        print(s)
        Logger.log(s)
        self.buffer = ""

