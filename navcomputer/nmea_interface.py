from nmea_encoder import encode_apb, encode_bwr, encode_rmb
from navigator import Navigator


class NmeaInterface:
    SERIAL_INSTRUMENTS = 0  # Instruments input, output to autopilot
    TCP_INSTRUMENTS_INPUT = 1  # Input of GPS and instruments data
    TCP_APP_CLIENTS = 2  # Applications like Open CPN
    NMEA_STATE_WAIT_SOP = 1
    NMEA_STATE_WAIT_EOP = 2

    def __init__(self, file, interface_type, nmea_parser, instr_inputs):
        self.file = file
        self.interface_type = interface_type
        self.nmea_parser = nmea_parser
        self.nmea_state = NmeaInterface.NMEA_STATE_WAIT_SOP
        self.nmea_sentence = ""
        self.instr_inputs = instr_inputs
        self.nmea_listeners = []
        if interface_type in [NmeaInterface.SERIAL_INSTRUMENTS, NmeaInterface.TCP_APP_CLIENTS]:
            Navigator.get_instance().add_listener(self)

        # Subscribe to the feed of of NMEA instrument inputs
        for instr_input in self.instr_inputs:
            instr_input.add_nmea_listener(self)

    def add_nmea_listener(self, listener):
        self.nmea_listeners.append(listener)

    def remove_nmea_listener(self, listener):
        self.nmea_listeners.remove(listener)

    def on_nmea(self, nmea):
        if self.interface_type == NmeaInterface.TCP_APP_CLIENTS:
            self.file.send(bytes(nmea, 'utf-8'))

    # Called by navigator when destination information is updated
    def on_dest_info(self, raw_instr_data, dest_info):
        apb = encode_apb(dest_info)
        rmb = encode_rmb(dest_info)
        bwr = encode_bwr(raw_instr_data, dest_info)

        if self.interface_type == NmeaInterface.TCP_APP_CLIENTS:
            self.file.send(bytes(apb, 'utf-8'))
            self.file.send(bytes(rmb, 'utf-8'))
            self.file.send(bytes(bwr, 'utf-8'))

    def read(self):
        if self.interface_type == self.SERIAL_INSTRUMENTS:
            data = self.file.read(10)  # Should be ready
            if data:
                self.set_nmea_data(data)
                return data
            else:
                print('Lost connection to ', self.file)
                Navigator.get_instance().remove_listener(self)
                for instr_input in self.instr_inputs:
                    instr_input.remove_nmea_listener(self)
                return None
        else:
            data = self.file.recv(10)  # Should be ready
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
            if self.nmea_state == NmeaInterface.NMEA_STATE_WAIT_SOP:
                if c == '$':
                    self.nmea_sentence += c
                    self.nmea_state = NmeaInterface.NMEA_STATE_WAIT_EOP
            else:
                if c == '\r' or c == '\n':
                    for listener in self.nmea_listeners:
                        listener.on_nmea(self.nmea_sentence + '\r\n')
                    self.nmea_parser.set_nmea_sentence(self.nmea_sentence)
                    self.nmea_sentence = ''
                    self.nmea_state = NmeaInterface.NMEA_STATE_WAIT_SOP
                else:
                    self.nmea_sentence += c
