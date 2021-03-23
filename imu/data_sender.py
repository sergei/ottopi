import json
import math

import websocket

LOCAL_SIGNAL_K_URL = "ws://localhost:3000/signalk/v1/stream"
LOCAL_NAVCOMPUTER_URL = "ws://localhost:5555/signalk/v1/stream"


class DataSender:
    def __init__(self, url=LOCAL_SIGNAL_K_URL):
        self.url = url
        self.ws = websocket.WebSocket()
        self.is_connected = False

    def connect(self):
        self.ws.connect(self.url)

    def close(self):
        self.ws.close()

    def send(self, heading, pitch, roll):
        if not self.is_connected:
            try:
                self.connect()
                self.is_connected = True
            except Exception as e:
                print(e)
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
            print(e)


if __name__ == '__main__':
    # data_sender = DataSender()
    data_sender = DataSender(LOCAL_NAVCOMPUTER_URL)
    data_sender.send(270, 10, -10)
