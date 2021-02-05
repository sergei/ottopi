import subprocess
import time


class Speaker:
    __instance = None

    @staticmethod
    def get_instance():
        """ Static access method """
        if Speaker.__instance is None:
            Speaker()
        return Speaker.__instance

    def __init__(self):
        """ Virtually private constructor.  """
        if Speaker.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            Speaker.__instance = self
            self.last_dest_announced_at = time.time()
            self.espeak_bin = '/usr/local/bin/espeak'

    def on_dest_info(self, dest_info):
        if time.time() - self.last_dest_announced_at >= 60:
            if dest_info.atw_up is not None:
                s = 'Mark {} is {:.0f} degrees {}'.format(dest_info.wpt.name,
                                                          abs(dest_info.atw),
                                                          'up' if dest_info.atw_up else 'down')
            else:
                s = 'Mark {} is {:.0f} degrees to the {}'.format(dest_info.wpt.name,
                                                                 abs(dest_info.atw),
                                                                 'right' if dest_info.atw > 0 else 'left')
            self.say(s)
            self.last_dest_announced_at = time.time()
            return s
        else:
            return None

    def say(self, s):
        print('Saying ' + s)
        cmd = [self.espeak_bin, '"' + s + '"']
        subprocess.run(cmd)
