import json
import math
import os
import time
import uuid

import requests
import websocket

SIGNAL_K_HOST = 'http://localhost:3000'
SIGNAL_K_ACCESS_URL = SIGNAL_K_HOST + "/signalk/v1/access/requests"
LOCAL_SIGNAL_K_URL = "ws://localhost:3000/signalk/v1/stream"
LOCAL_NAVCOMPUTER_URL = "ws://localhost:5555/signalk/v1/stream"


class DataSender:
    def __init__(self, url, data_dir):
        self.url = url
        self.ws = websocket.WebSocket()
        self.is_connected = False
        self.config_file = os.path.expanduser(data_dir + os.sep + 'signalk-auth.json')
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
            self.store_cfg()

    def store_cfg(self):
        with open(self.config_file, 'w') as f:
            json.dump(self.config, f)

    def request_access(self):
        data = {"clientId": self.config['client_id'],
                "description": "OttoPi IMU"
                }
        r = requests.post(SIGNAL_K_ACCESS_URL, json=data)
        print(r)
        resp = json.loads(r.text)
        if 'href' in resp:
            self.store_cfg()
            print('Waiting for approval')
            while True:
                time.sleep(5)
                r = requests.get(SIGNAL_K_HOST + resp['href'])
                print(r)
                resp = json.loads(r.text)
                if resp['statusCode'] == 400:  # Already approved
                    break
                if resp['statusCode'] == 200:
                    if resp['accessRequest']['permission'] == 'APPROVED':
                        self.config['token'] = resp['accessRequest']['token']
                        self.store_cfg()
                        break

    def connect(self):
        while self.config['token'] is None:
            try:
                self.request_access()
            except Exception as e:
                print(e)

        try:
            self.ws.connect(self.url, header=[
                'Authorization: Bearer {}'.format(self.config['token'])
            ])
        except websocket.WebSocketBadStatusException as e:
            if e.status_code == 401:
                self.config['token'] = None
            print('WebSocketBadStatusException ' + str(e))
            raise e

        # Read hello
        r = self.ws.recv()
        print('Hello:' + r)

    def close(self):
        self.ws.close()

    def send(self, heading, pitch, roll):
        if not self.is_connected:
            try:
                self.connect()
                self.is_connected = True
            except Exception as e:
                print('Connect error' + str(e))
                return

        data = {
            "updates": [
                {
                    "source": {
                        "type": "I2C",
                        "label": "ICM20948",
                        "src": "68"
                    },
                    "values": [
                        {
                            "path": "navigation.attitude",
                            "value": {
                                'roll': math.radians(roll),
                                'pitch': math.radians(pitch),
                                'yaw': math.radians(heading),
                            }
                        }
                    ]
                }
            ]
        }

        try:
            self.ws.send(json.dumps(data))
        except Exception as e:
            self.is_connected = False
            print('Send error' + str(e))

    def authorize(self):
        pass


if __name__ == '__main__':
    data_sender = DataSender(LOCAL_NAVCOMPUTER_URL, '.')
    data_sender.send(270, 10, -10)
