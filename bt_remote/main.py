import argparse

from bang_acp_rest_client import BangAcpRestClient
from bt_remote_cli import CmdLineBtRemote


def main(args):
    acp_bt_remote = CmdLineBtRemote(args.bt_addr_acp)
    acp_rest_client = BangAcpRestClient()

    acp_bt_remote.connect()
    acp_bt_remote.poll_device(acp_rest_client.on_remote_key)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--bt-addr-acp", help="BT address of remote controller to control ACP ",  required=True)

    main(parser.parse_args())
