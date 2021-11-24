import yaml


class ExtraEvents:
    def __init__(self, yaml_name):
        self.events = []
        with open(yaml_name, 'r') as stream:
            try:
                self.events = yaml.safe_load(stream)
            except yaml.YAMLError as exc:
                print(exc)


if __name__ == '__main__':
    extra_events = ExtraEvents('args/extra_evts.yaml')
    print(extra_events)
