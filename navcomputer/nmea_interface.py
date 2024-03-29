from serial import SerialException

from bang_logger import BangLogger
from navigator_listener import NavigationListener
from nmea_encoder import encode_apb, encode_bwr, encode_rmb
from navigator import Navigator


class NmeaInterface(NavigationListener):
    SERIAL_NMEA_GPS = 0        # GPS NMEA input
    SERIAL_NMEA_INSTR = 1      # Instruments input, output to autopilot
    TCP_INSTRUMENTS_INPUT = 2  # Input of GPS and instruments data
    TCP_APP_CLIENTS = 3        # Applications like Open CPN
    SERIAL_BANG_NET = 4        # B&G NET input

    NMEA_STATE_WAIT_SOP = 1
    NMEA_STATE_WAIT_EOP = 2

    def __init__(self, file, interface_type, nmea_parser, instr_inputs):
        super().__init__()
        self.bang_logger = BangLogger()
        self.file = file
        self.interface_type = interface_type
        self.nmea_parser = nmea_parser
        self.nmea_state = self.NMEA_STATE_WAIT_SOP
        self.nmea_sentence = ""
        self.instr_inputs = instr_inputs
        self.nmea_listeners = []
        if interface_type in [self.SERIAL_NMEA_INSTR, self.TCP_APP_CLIENTS]:
            Navigator.get_instance().add_listener(self)

        # Subscribe to the feed of NMEA instrument inputs
        for instr_input in self.instr_inputs:
            instr_input.add_nmea_listener(self)

    def add_nmea_listener(self, listener):
        self.nmea_listeners.append(listener)

    def remove_nmea_listener(self, listener):
        self.nmea_listeners.remove(listener)

    IGNORE_MSGS = ['GLL', 'GSA', 'GSV', 'GGA', 'PGLOR', 'VTG']

    def on_nmea(self, nmea):
        # Ignore unwanted GPS messages
        t = nmea.split()
        nmea_msg = t[0]
        for msg in self.IGNORE_MSGS:
            if msg in nmea_msg:
                return

        if self.interface_type == self.TCP_APP_CLIENTS:
            try:
                self.file.send(bytes(nmea, 'utf-8'))
            except IOError as e:
                print('Should not see it {}'.format(e))  # TODO might consider closing this connection
        elif self.interface_type == self.SERIAL_NMEA_INSTR:
            try:
                self.file.write(bytes(nmea, 'utf-8'))
            except IOError as e:
                print('Should not see it {}'.format(e))  # TODO might consider closing this connection

    # Called by navigator when destination information is updated
    def on_dest_info(self, raw_instr_data, dest_info):
        apb = encode_apb(dest_info)
        rmb = encode_rmb(dest_info)
        bwr = encode_bwr(raw_instr_data, dest_info)

        if self.interface_type in [self.TCP_APP_CLIENTS]:
            self.file.send(bytes(apb, 'ascii'))
            self.file.send(bytes(rmb, 'ascii'))
            self.file.send(bytes(bwr, 'ascii'))

        if self.interface_type in [self.SERIAL_NMEA_INSTR]:
            self.file.write(bytes(apb, 'ascii'))
            self.file.write(bytes(rmb, 'ascii'))
            self.file.write(bytes(bwr, 'ascii'))

    def read(self):
        if self.interface_type in [self.SERIAL_NMEA_GPS, self.SERIAL_NMEA_INSTR]:
            try:
                data = self.file.read(1)  # Should be ready
                if data:
                    self.set_nmea_data(data)
                    return data
            except TimeoutError as e:
                print('Should never happen {}'.format(e))
                return None
            except SerialException as e:
                print('Should never happen {}'.format(e))
                return None

            else:
                print('Lost connection to ', self.file)
                Navigator.get_instance().remove_listener(self)
                for instr_input in self.instr_inputs:
                    instr_input.remove_nmea_listener(self)
                return None
        elif self.interface_type in [self.SERIAL_BANG_NET]:
            try:
                data = self.file.read(1)  # Should be ready
            except TimeoutError as e:
                print('Should never happen {}'.format(e))
                return None

            if data:
                self.bang_logger.log(data)
                return data
            else:
                print('Lost connection to ', self.file)
                return None
        else:
            try:
                data = self.file.recv(1)  # Should be ready
            except Exception as e:
                print('Should never happen {}'.format(e))
                print('Lost connection to ', self.file)
                Navigator.get_instance().remove_listener(self)
                for instr_input in self.instr_inputs:
                    instr_input.remove_nmea_listener(self)
                return None
            if data:
                self.set_nmea_data(data)
                return data
            else:
                print('Lost connection to ', self.file)
                Navigator.get_instance().remove_listener(self)
                for instr_input in self.instr_inputs:
                    instr_input.remove_nmea_listener(self)
                return None

    def set_nmea_data(self, data):
        for c in data.decode('ascii', errors='ignore'):
            if self.nmea_state == self.NMEA_STATE_WAIT_SOP:
                if c == '$':
                    self.nmea_sentence += c
                    self.nmea_state = self.NMEA_STATE_WAIT_EOP
            else:
                if c == '\r' or c == '\n':
                    for listener in self.nmea_listeners:
                        listener.on_nmea(self.nmea_sentence + '\r\n')
                    self.nmea_parser.set_nmea_sentence(self.nmea_sentence)
                    self.nmea_sentence = ''
                    self.nmea_state = self.NMEA_STATE_WAIT_SOP
                else:
                    self.nmea_sentence += c
