import requests


class RestClient:
    """
    Use this class to communicate with otto pi navigator
    """
    def __init__(self):
        self.end_point_url = 'http://localhost/'

    def post(self, path, data):
        print(self.end_point_url + path)
        r = requests.post(self.end_point_url + path, data=data)
        print(r)
