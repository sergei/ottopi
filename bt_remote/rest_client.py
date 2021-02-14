import json
from json import JSONDecodeError

import requests


class RestClient:
    """
    Use this class to communicate with otto pi navigator
    """
    def __init__(self, url=None):
        self.end_point_url = 'http://localhost/' if url is None else url

    def post(self, path, data):
        print(self.end_point_url + path)
        r = requests.post(self.end_point_url + path, json=data)
        print(r)

    def get(self, path):
        print(self.end_point_url + path)
        r = requests.get(self.end_point_url + path)

        try:
            data = json.loads(r.text)
            return data
        except JSONDecodeError:
            return None
