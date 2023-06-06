import json
import string
from datetime import datetime
from uuid import uuid4


class ClipEvent:
    uuid: string
    name: string
    utc_from: datetime
    utc_to: datetime
    utc_gun: datetime

    def __init__(self, name: string, utc_from: datetime, utc_to: datetime, utc_gun: datetime = None):
        self.uuid = str(uuid4())
        self.name = name
        self.utc_from = utc_from
        self.utc_to = utc_to
        self.utc_gun = utc_gun

    def to_dict(self):
        return {
            'uuid': self.uuid,
            'name': self.name,
            'utc_from': self.utc_from.isoformat(),
            'utc_to': self.utc_to.isoformat(),
            'utc_gun': None if self.utc_gun is None else self.utc_gun.isoformat()
        }

    @classmethod
    def from_dict(cls, d):
        evt = ClipEvent(d['name'], d['utc_from'], d['utc_to'],  d['utc_gun'] if 'utc_gun' in d else None)
        evt.uuid = d['uuid']
        return evt

    @staticmethod
    def decode_obj(d):
        for k in d:
            if 'utc_from' in k or 'utc_to' in k or 'utc_gun' in k:
                if d[k] is None:
                    d[k] = None
                else:
                    d[k] = datetime.fromisoformat(d[k])

        return d

    @classmethod
    def from_json(cls, s):
        d = json.loads(s, object_hook=cls.decode_obj)
        return ClipEvent.from_dict(d)

    def to_json(self):
        return json.dumps(self.to_dict())
