import json
import string
from datetime import datetime
from uuid import uuid4

from gui.clip_event import ClipEvent


class RaceInfo:
    uuid: string
    name: string
    utc_from: datetime
    utc_to: datetime
    events: list[ClipEvent]

    def __init__(self, name: string, utc_from: datetime, utc_to: datetime, events: list[ClipEvent]):
        self.uuid = str(uuid4())
        self.name = name
        self.utc_from = utc_from
        self.utc_to = utc_to
        self.events = events

    def to_dict(self):
        return {
            'uuid': self.uuid,
            'name': self.name,
            'utc_from': self.utc_from.isoformat(),
            'utc_to': self.utc_to.isoformat(),
            'events': [event.to_dict() for event in self.events]
        }

    @classmethod
    def from_dict(cls, d, events):
        evt = RaceInfo(d['name'], d['utc_from'], d['utc_to'], events)
        evt.uuid = d['uuid']
        return evt

    @staticmethod
    def decode_obj(d):
        for k in d:
            if 'utc_from' in k or 'utc_to' in k:
                if d[k] is None:
                    d[k] = None
                else:
                    d[k] = datetime.fromisoformat(d[k])

        return d

    @classmethod
    def from_json(cls, s):
        d = json.loads('{' + s + '}', object_hook=cls.decode_obj)
        return RaceInfo.from_dict(d)

    def to_json(self):
        return json.dumps(self.to_dict())
