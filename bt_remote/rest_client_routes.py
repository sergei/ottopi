from bt_remote import BtRemote
from rest_client import RestClient


class RoutesRestClient(RestClient):
    """
    Use this class to map BT remote control keys to ottopi  B&G autopilot REST APIs
    """
    def __init__(self, url=None):
        super().__init__(url)
        self.routes = []
        self.active_route_idx = -1
        self.active_wpt_idx = -1

    def get_routes(self):
        routes = self.get('routes')
        self.routes = routes if routes is not None else []
        # Find active route and active wpt
        self.active_route_idx = 0
        self.active_wpt_idx = 0
        for rte_idx, route in enumerate(self.routes):
            if 'active' in route and route['active']:
                self.active_route_idx = rte_idx
                if 'active_wpt_idx' in route:
                    self.active_wpt_idx = route['active_wpt_idx']

        print(self.routes)
        return routes

    def on_remote_key(self, key):
        if key == BtRemote.PLAY_BUTTON:  # Select an go
            self.post('announce_current_route', {})
        elif key == BtRemote.PLUS_BUTTON:  # Next route
            self.routes = self.get_routes()
            num_routes = len(self.routes)
            if num_routes > 0:
                self.active_route_idx = (self.active_route_idx + 1) % num_routes
                self.active_wpt_idx = 0
                self.post_route(self.routes[self.active_route_idx])
        elif key == BtRemote.MINUS_BUTTON:  # Previous route
            self.routes = self.get_routes()
            num_routes = len(self.routes)
            if num_routes > 0:
                self.active_wpt_idx = 0
                self.active_route_idx = (self.active_route_idx - 1) % num_routes
                self.post_route(self.routes[self.active_route_idx])
        elif key == BtRemote.NEXT_BUTTON:  # Next waypoint
            self.routes = self.get_routes()
            num_routes = len(self.routes)
            if num_routes > 0:
                num_wpts = len(self.routes[self.active_route_idx]['wpts'])
                if num_wpts > 0:
                    self.active_wpt_idx = (self.active_wpt_idx + 1) % num_wpts
                    self.post_route(self.routes[self.active_route_idx])
        elif key == BtRemote.PREV_BUTTON:  # Previous waypoint
            self.routes = self.get_routes()
            num_routes = len(self.routes)
            if num_routes > 0:
                num_wpts = len(self.routes[self.active_route_idx]['wpts'])
                if num_wpts > 0:
                    self.active_wpt_idx = (self.active_wpt_idx - 1) % num_wpts
                    self.post_route(self.routes[self.active_route_idx])

    def post_route(self, route):
        # Set active WPT
        route['active_wpt_idx'] = self.active_wpt_idx
        self.post('routes/active_route', route)


if __name__ == '__main__':
    from sim_bt_remote import sim_bt_remote
    sim_bt_remote(RoutesRestClient('http://localhost:5555/'))
