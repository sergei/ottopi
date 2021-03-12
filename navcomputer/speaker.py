import os
import subprocess

from navigator_listener import NavigationListener
from logger import Logger


class Speaker(NavigationListener):
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
            super().__init__()
            Speaker.__instance = self
            if os.path.isfile('/usr/local/bin/espeak'):
                self.espeak_bin = '/usr/local/bin/espeak'
            elif os.path.isfile('/usr/bin/espeak'):
                self.espeak_bin = '/usr/bin/espeak'
            else:
                self.espeak_bin = None

            if os.path.isfile('/usr/bin/afplay'):
                self.player_bin = '/usr/bin/afplay'
            elif os.path.isfile('omxplayer'):
                self.player_bin = 'omxplayer'
            else:
                self.player_bin = None

    def on_speech(self, speech):
        print('Saying ' + speech)
        Logger.log('> $POTTOPI,SAY,{}'.format(speech))

        cmd = [self.espeak_bin, '"' + speech + '"']
        subprocess.run(cmd)

    def play_file(self, file):
        print('Playing ' + file)
        Logger.log('> $POTTOPI,SAY,{}'.format(file))

        cmd = [self.player_bin,  file]
        subprocess.run(cmd)
