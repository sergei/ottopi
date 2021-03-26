import json
import os
import threading
import time
import uuid
from json import JSONDecodeError
from dateutil.parser import parse

import requests
import websocket
from typing import List


class SkClientListener:
    def on_update_from_sk(self, utc, values):
        pass


class SkClient:
    SIGNAL_K_ACCESS_URL = "/signalk/v1/access/requests"

    server: str
    ws: websocket.WebSocket
    w_sock: websocket.WebSocketApp
    websocket_t: threading.Thread
    is_connected: bool
    config_file: str
    config: dict
    listeners: List[SkClientListener]

    # noinspection PyTypeChecker
    def __init__(self, host, data_dir='/tmp'):
        self.server = host
        self.ws = websocket.WebSocket()
        self.w_sock = None
        self.websocket_t = None
        self.is_connected = False
        self.config_file = os.path.expanduser(data_dir + os.sep + 'signalk-auth.json')
        self.listeners = []
        self.config = {
            'client_id': None,
            'token': None
        }
        try:
            with open(self.config_file, 'r') as f:
                self.config = json.load(f)
        except Exception as e:
            print(e)
        if self.config['client_id'] is None:
            self.config['client_id'] = str(uuid.uuid4())
            self.__store_cfg()

    def __store_cfg(self):
        with open(self.config_file, 'w') as f:
            json.dump(self.config, f)

    def __config(self):
        """discover endpoints from server
        """

        import requests

        print("Attempting Connection to {}...".format(self.server))
        # Discover API and Stream endpoints
        data = requests.get("http://%s/signalk" % self.server).json()
        endpoints = data['endpoints']['v1']
        print("Connected to SignalK Server({})".format(
            endpoints['version']
            ))

        self.api_endpoint = endpoints['signalk-http']
        self.stream_endpoint = endpoints['signalk-ws']

        print("Got endpoints: api_endpoint={} stream_endpoint={}".format(
            self.api_endpoint,
            self.stream_endpoint,
            ))

    def request_access(self):
        data = {"clientId": self.config['client_id'],
                "description": "OttoPi IMU"
                }
        url = 'http://' + self.server + self.SIGNAL_K_ACCESS_URL
        r = requests.post(url, json=data)
        print(r)
        resp = json.loads(r.text)
        if 'href' in resp:
            self.__store_cfg()
            print('Waiting for approval')
            while True:
                time.sleep(5)
                r = requests.get('http://' + self.server + resp['href'])
                print(r)
                resp = json.loads(r.text)
                if resp['statusCode'] == 400:  # Already approved
                    break
                if resp['statusCode'] == 200:
                    if resp['accessRequest']['permission'] == 'APPROVED':
                        self.config['token'] = resp['accessRequest']['token']
                        self.__store_cfg()
                        break

    def add_listener(self, listener: SkClientListener):
        self.listeners.append(listener)

    def remove_listener(self, listener: SkClientListener):
        self.listeners.remove(listener)

    def subscribe(self):
        self.__config()

        self.w_sock = websocket.WebSocketApp(
            "%s?subscribe=all" % self.stream_endpoint,
            on_message=self.__ws_on_message,
            on_error=self.__ws_on_error,
            on_close=self.__ws_on_close,
            on_open=self.__ws_on_open
            )

        self.websocket_t = threading.Thread(target=self.w_sock.run_forever)
        self.websocket_t.daemon = True
        self.websocket_t.start()
        # self.websocket_t.join()

    # noinspection PyUnusedLocal
    def __ws_on_message(self, w_sock, message):
        """websocket message handler"""
        try:
            data = json.loads(message)
            if 'updates' in data:
                for update in data['updates']:
                    utc = parse(update['timestamp'])
                    if 'values' in update:
                        self.on_update(utc, update['values'])

        except JSONDecodeError as e:
            print(e)

    def __ws_on_error(self, w_sock, error):
        """websocket error handler"""
        pass

    def __ws_on_close(self, w_sock):
        """websocket connection close handler"""
        pass

    def __ws_on_open(self, w_sock):
        """websocket connection open handler"""

    def on_update(self, utc, values):
        for listener in self.listeners:
            listener.on_update_from_sk(utc, values)


if __name__ == '__main__':
    client = SkClient('localhost:3000')
    client.subscribe()
