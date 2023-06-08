import socket  # for UDP broadcast

from n2k_broadcast import make_can_frame


class N2kBroadcaster:
    def __init__(self, udp_port=2024):
        self.udp_port = udp_port
        self.s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    def send_epoch(self, n2k_epoch):
        if n2k_epoch is None:
            return
        for n2k_msk in n2k_epoch:
            can_frame = make_can_frame(n2k_msk)
            self.s.sendto(can_frame, ('<broadcast>', self.udp_port))
