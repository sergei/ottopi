import argparse
import requests

from bt_ctrl import BtController
from bt_ctrl_cli import CmdLineBtController


def on_event(event):
    print(event)
    if event == BtController.PLAY_BUTTON:
        requests.post('http://localhost/autopilot/tack', data={})
    elif event == BtController.NEXT_BUTTON:
        requests.post('http://localhost/autopilot/steer/10', data={})
    elif event == BtController.PREV_BUTTON:
        requests.post('http://localhost/autopilot/steer/-10', data={})
    elif event == BtController.PLUS_BUTTON:
        requests.post('http://localhost/autopilot/steer/2', data={})
    elif event == BtController.MINUS_BUTTON:
        requests.post('http://localhost/autopilot/steer/-2', data={})


def bt_remote(args):
    acp_controller = CmdLineBtController(args.bt_addr_acp)
    acp_controller.connect()

    acp_controller.poll_device(on_event)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--bt-addr-acp", help="BT address of remote controller to control ACP ",  required=True)

    bt_remote(parser.parse_args())
