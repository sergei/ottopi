import copy
import threading

from raw_instr_data import RawInstrData


class DataRegistry:
    __instance = None

    @staticmethod
    def get_instance():
        """ Static access method """
        if DataRegistry.__instance is None:
            DataRegistry()
        return DataRegistry.__instance

    def __init__(self):
        """ Virtually private constructor.  """
        if DataRegistry.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            DataRegistry.__instance = self
            self.raw_instr_data = RawInstrData()
            self.lock = threading.Lock()

    def set_raw_instr_data(self, raw_instr_data):
        with self.lock:
            self.raw_instr_data = raw_instr_data

    def get_raw_instr_data_dict(self):
        with self.lock:
            raw_instr_data = copy.copy(self.raw_instr_data)

        if raw_instr_data is not None:
            return raw_instr_data.__dict__
        else:
            return {}
