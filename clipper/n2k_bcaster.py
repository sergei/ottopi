import socket  # for UDP broadcast
import queue
import threading
from time import sleep

from n2k_broadcast import make_can_frame


class N2kBroadcaster:
    def __init__(self, udp_port=2024):
        self.udp_port = udp_port
        self.s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.q = queue.Queue()
        self.last_t_ms = 0

    def start_udp_thread(self):
        thread = threading.Thread(target=self.udp_thread)
        thread.start()

    def udp_thread(self):
        print(f'Starting UDP broadcast on port {self.udp_port}')
        while True:
            n2k_msg = self.q.get()
            t = n2k_msg[0]
            dt_ms = (t - self.last_t_ms)
            if 0 < dt_ms < 2000:
                sleep(dt_ms * 0.001)
            self.last_t_ms = t
            can_frame = make_can_frame(n2k_msg[1])
            self.s.sendto(can_frame, ('<broadcast>', self.udp_port))

    def send_epoch(self, n2k_epoch):
        if n2k_epoch is None:
            return
        for n2k_msg in n2k_epoch:
            self.q.put(n2k_msg)
