import re
import subprocess
import threading
from multiprocessing import Process
import time

from bt_remote import BtRemote


class CmdLineBtRemote(BtRemote):
    """
    Very naive controller using the command line utilities of BlueZ
    for instance to read the events form the remote controller the BT bus sniffer btmon is used
    """

    BUTTONS_MAP = {
        59904: BtRemote.MINUS_BUTTON,
        59648: BtRemote.PLUS_BUTTON,
        46592: BtRemote.PREV_BUTTON,
        46336: BtRemote.NEXT_BUTTON,
        52480: BtRemote.PLAY_BUTTON,
        12288: BtRemote.VENDOR_BUTTON,
    }

    def __init__(self, addr):
        super().__init__(addr)

    @staticmethod
    def run_command(cmd, regex):
        success = False
        print('executing [{}]'.format(' '.join(cmd)))
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE)
        while True:
            output = process.stdout.readline().decode()
            if output == '' and process.poll() is not None:
                break
            if output:
                print(output.strip())
                if re.search(regex, output) is not None:
                    success = True
        process.poll()
        return success

    def monitor_connection(self):
        while True:
            cmd = ['sudo', '/usr/bin/bluetoothctl', 'info', self.addr]
            is_connected = self.run_command(cmd, 'Connected.*yes')
            if is_connected:
                print('Device is already connected')
            else:
                print('Device is not connected')
                cmd = ['sudo', '/usr/bin/bluetoothctl', 'connect', self.addr]
                is_connected = self.run_command(cmd, 'Connection successful')
                if is_connected:
                    print('Connected')
                else:
                    print('Connection failed')
            sleep_sec = 30
            print('Sleeping for {}'.format(sleep_sec))
            time.sleep(sleep_sec)

    def connect(self):
        p = Process(target=self.monitor_connection, args=())
        p.start()

    def poll_device(self, event_handler):
        t = threading.Thread(target=self.__poll_device, name='flask_server', args=[event_handler], daemon=True)
        t.start()
        return t

    def __poll_device(self, event_handler):
        # Without sdbuf there is huge buffering in the pipe between btmon and the pipe, so we don't get all events
        cmd = ['sudo'] + ['stdbuf', '-o0'] + ['btmon', '--no-pager']
        print('executing [{}]'.format(' '.join(cmd)))
        process = subprocess.Popen(cmd, bufsize=0, stdout=subprocess.PIPE, universal_newlines=True)
        wait_for_data = False
        prev_scan_code = None
        while True:
            output = process.stdout.readline()
            if output == '' and process.poll() is not None:
                break
            if output:
                if re.search('ACL Data RX.*Handle 64', output) is not None:
                    wait_for_data = True
                if wait_for_data and re.search('Data:', output):
                    t = output.split()
                    if len(t) > 1:
                        try:
                            scan_code = int(t[1], 16)
                            if scan_code == 0 and prev_scan_code is not None:
                                event = self.BUTTONS_MAP.get(prev_scan_code)
                                if event is not None:
                                    event_handler(event)
                            prev_scan_code = scan_code
                        except ValueError:
                            pass
                    wait_for_data = False

        process.poll()
