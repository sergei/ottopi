from bt_remote import BtRemote
from rest_client import RestClient


class RoutesRestClient(RestClient):
    """
    Use this class to map BT remote control keys to ottopi  B&G autopilot REST APIs
    """
    def __init__(self, url=None):
        super().__init__(url)
        self.routes = []
        self.active_route_idx = 0
        self.active_wpt_idx = 0

    def get_routes(self):
        routes = self.get('routes')
        self.routes = routes if routes is not None else []
        # Find active route and active wpt
        self.active_route_idx = 0
        self.active_wpt_idx = 0
        for rte_idx, route in enumerate(self.routes):
            if 'active' in route:
                self.active_route_idx = rte_idx
                for wpt_idx, wpt in enumerate(route['wpts']):
                    if 'active' in wpt:
                        self.active_wpt_idx = wpt_idx

    def on_remote_key(self, key):
        if key == BtRemote.PLAY_BUTTON:  # Select an go
            self.post('announce_current_route', {})
        elif key == BtRemote.NEXT_BUTTON:  # Next route
            self.routes = self.get('routes')
            num_routes = len(self.routes)
            if num_routes > 0:
                self.active_route_idx = (self.active_route_idx + 1) % num_routes
                self.post_route(self.routes[self.active_route_idx])
        elif key == BtRemote.PREV_BUTTON:  # Previous route
            self.routes = self.get('routes')
            num_routes = len(self.routes)
            if num_routes > 0:
                self.active_route_idx = (self.active_route_idx - 1) % num_routes
                self.post_route(self.routes[self.active_route_idx])
        elif key == BtRemote.PLUS_BUTTON:  # Next waypoint
            self.routes = self.get('routes')
            num_routes = len(self.routes)
            if num_routes > 0:
                num_wpts = len(self.routes[self.active_route_idx]['wpts'])
                if num_wpts > 0:
                    self.active_wpt_idx = (self.active_wpt_idx + 1) % num_wpts
                    self.post_route(self.routes[self.active_route_idx])
        elif key == BtRemote.MINUS_BUTTON:  # Previous waypoint
            self.routes = self.get('routes')
            num_routes = len(self.routes)
            if num_routes > 0:
                num_wpts = len(self.routes[self.active_route_idx]['wpts'])
                if num_wpts > 0:
                    self.active_wpt_idx = (self.active_wpt_idx - 1) % num_wpts
                    self.post_route(self.routes[self.active_route_idx])

    def post_route(self, route):
        # Set active WPT
        route['active_wpt_idx'] = self.active_wpt_idx
        self.post('select_route', route)
