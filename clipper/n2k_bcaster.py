import socket  # for UDP broadcast
import queue
import threading
from time import sleep

from n2k_broadcast import make_can_frame
from raw_instr_data import RawInstrData


class N2kBroadcaster:
    def __init__(self, udp_port=2024):
        self.udp_port = udp_port
        self.s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.q = queue.Queue()

    def start_udp_thread(self):
        thread = threading.Thread(target=self.udp_thread)
        thread.start()

    def udp_thread(self):
        print(f'Starting UDP broadcast on port {self.udp_port}')
        while True:
            instr_data_epoch = self.q.get()
            n2k_msgs = []
            for instr_data in instr_data_epoch:
                for n2k_msg in instr_data.n2k_epoch:
                    n2k_msgs.append(n2k_msg)

            dt = 0.8 / len(n2k_msgs)  # We have one second to send these messages, but let's use only 800ms
            for n2k_msg in n2k_msgs:
                can_frame = make_can_frame(n2k_msg[1])
                self.s.sendto(can_frame, ('<broadcast>', self.udp_port))
                sleep(dt)

    def send_epoch(self, instr_data_epoch: list[RawInstrData]):
        if self.q.qsize() > 10:
            print(f'Queue too long {self.q.qsize()}, clear it')
            self.q.queue.clear()
        self.q.put(instr_data_epoch)
