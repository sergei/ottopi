import argparse

from bang_acp_rest_client import BangAcpRestClient
from bt_remote_cli import CmdLineBtRemote
from bt_remote_hog import HogBtRemote
from routes_rest_client import RoutesRestClient


def main(args):

    threads = []

    if args.bt_addr_acp is not None:
        acp_bt_remote = CmdLineBtRemote(args.bt_addr_acp)
        acp_bt_remote.connect()
        acp_rest_client = BangAcpRestClient()
        t = acp_bt_remote.poll_device(acp_rest_client.on_remote_key)
        threads.append(t)

    if args.bt_addr_route:
        routes_client = RoutesRestClient()
        routes_bt_remote = HogBtRemote(args.bt_addr_route)
        t = routes_bt_remote.poll_device(routes_client.on_remote_key)
        threads.append(t)

    for t in threads:
        t.join()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--bt-addr-acp", help="BT address of remote controller to control ACP ",  required=False)
    parser.add_argument("--bt-addr-route", help="BT address of remote controller to control Routes selection ",
                        required=False)

    main(parser.parse_args())
