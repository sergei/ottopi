import re
import subprocess

from bt_ctrl import BtController


class CmdLineBtController(BtController):
    """
    Very naive controller using the command line utilities of BlueZ
    for instance to read the events form the remote controller the BT bus sniffer btmon is used
    """

    BUTTONS_MAP = {
        59904: BtController.MINUS_BUTTON,
        59648: BtController.PLUS_BUTTON,
        46592: BtController.PREV_BUTTON,
        46336: BtController.NEXT_BUTTON,
        52480: BtController.PLAY_BUTTON,
        12288: BtController.VENDOR_BUTTON,
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

    def connect(self):
        cmd = ['sudo', '/usr/bin/bluetoothctl', 'info', self.addr]
        is_connected = self.run_command(cmd, 'Connected.*yes')
        if is_connected:
            print('Device is already connected')
        else:
            print('Device is not connected')
            cmd = ['sudo', '/usr/bin/bluetoothctl', 'connect', self.addr]
            is_connected = self.run_command(cmd, 'Connection successful')

        return is_connected

    def poll_device(self, event_handler):
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
                if re.search('ACL Data RX', output) is not None:
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
