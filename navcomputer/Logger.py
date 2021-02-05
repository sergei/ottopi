import glob
import os
import threading

LOG_DURATION_SECONDS = 60
KEEP_MAX_LOGS = 3


class Logger:
    __instance = None

    @staticmethod
    def get_instance():
        """ Static access method """
        if Logger.__instance is None:
            Logger()
        return Logger.__instance

    def __init__(self):
        """ Virtually private constructor.  """
        if Logger.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            Logger.__instance = self
            self.lock = threading.Lock()
            self.log_dir = None
            self.current_log_utc = None
            self.log_name = None
            self.log_file = None

    @staticmethod
    def set_log_dir(log_dir):
        Logger.get_instance().__set_log_dir(log_dir)

    @staticmethod
    def set_utc(utc):
        if utc is not None:
            Logger.get_instance().__set_utc(utc)

    @staticmethod
    def log(s):
        if s.endswith('\n'):
            Logger.get_instance().__log(s)
        else:
            Logger.get_instance().__log(s + '\n')

    def __log(self, s):
        with self.lock:
            if self.log_file is not None:
                # noinspection PyBroadException
                try:
                    self.log_file.write(s)
                    print('Writing {}'.format(s))
                except Exception as e:
                    print(e)

    def __rotate_logs(self):
        log_list = sorted(glob.glob(self.log_dir + os.sep + 'log-*.nmea'))
        if len(log_list) > KEEP_MAX_LOGS:
            for i in range(0, len(log_list) - KEEP_MAX_LOGS):
                print('Deleting {}'.format(log_list[i]))
                os.unlink(log_list[i])

    def __open_log(self, utc):
        self.__rotate_logs()
        if self.log_file is not None:
            print('Closing {}'.format(self.log_name))
            self.log_file.close()

        self.log_name = self.log_dir + os.sep + 'log-{:04d}-{:02d}-{:02d}-{:02d}{:02d}{:02d}.nmea'.\
            format(utc.year, utc.month, utc.day, utc.hour, utc.minute, utc.second)
        print('Creating {}'.format(self.log_name))
        self.log_file = open(self.log_name, 'a')
        self.current_log_utc = utc

    def __set_utc(self, utc):
        with self.lock:
            if self.log_dir is not None:
                if self.current_log_utc is None:
                    self.__open_log(utc)
                else:
                    log_duration = (utc - self.current_log_utc).total_seconds()
                    if log_duration > LOG_DURATION_SECONDS:
                        self.__open_log(utc)

    def __set_log_dir(self, log_dir):
        with self.lock:
            log_dir = os.path.expanduser(log_dir)
            if not os.path.isdir(log_dir):
                os.makedirs(log_dir)
            if os.path.isdir(log_dir):
                print('Set log dir to {}'.format(log_dir))
                self.log_dir = log_dir
