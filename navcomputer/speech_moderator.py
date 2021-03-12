import datetime
from enum import Enum


class SpeechEntryType(Enum):
    NAV_EVENT = 1
    NAV_UPDATE = 2
    DEST_UPDATE = 3


class SpeechEntry:
    def __init__(self, entry_type: SpeechEntryType, utc: datetime, phrase: str):
        self.entry_type = entry_type
        self.utc = utc
        self.phrase = phrase


class SpeechModerator:
    MIN_TIME_TILL_SPEECH = 10
    UPDATE_INTERVALS = {
        SpeechEntryType.NAV_EVENT: 0,
        SpeechEntryType.NAV_UPDATE: 60,
        SpeechEntryType.DEST_UPDATE: 120,
    }

    TYPE_LIST = [SpeechEntryType.NAV_EVENT, SpeechEntryType.DEST_UPDATE, SpeechEntryType.NAV_UPDATE]
    STATUS_TYPE_LIST = [SpeechEntryType.DEST_UPDATE, SpeechEntryType.NAV_UPDATE]

    def __init__(self, speech_listeners):
        self.spoken_at = {
            SpeechEntryType.NAV_EVENT: None,
            SpeechEntryType.NAV_UPDATE: None,
            SpeechEntryType.DEST_UPDATE: None,
        }
        self.last_spoken_at = None
        self.last_entries = {}
        self.speech_listeners = speech_listeners

    def add_entry(self, speech_entry: SpeechEntry):
        self.last_entries[speech_entry.entry_type] = speech_entry

    def time_since_last_speech(self, entry_type: SpeechEntryType, utc_now: datetime):
        if self.spoken_at[entry_type] is not None:
            return (utc_now - self.spoken_at[entry_type]).total_seconds()
        else:
            return 3600

    def say_something(self, utc_now, in_pre_start):

        if in_pre_start:  # Stay silent in pre start sequence
            return

        if self.last_spoken_at is not None:
            time_since_last_speech = (utc_now - self.last_spoken_at).total_seconds()
        else:
            time_since_last_speech = 3600

        for entry_type in self.TYPE_LIST:
            if entry_type in self.last_entries:
                if self.time_since_last_speech(entry_type, utc_now) >= self.UPDATE_INTERVALS[entry_type]:
                    if entry_type in self.STATUS_TYPE_LIST and time_since_last_speech < self.MIN_TIME_TILL_SPEECH:
                        break

                    # Say the phrase
                    self.say_now(self.last_entries[entry_type].phrase)

                    # Remove what we just said
                    self.last_entries.pop(entry_type)

                    # Update time when we spoke last time
                    self.last_spoken_at = utc_now
                    self.spoken_at[entry_type] = utc_now

                    # Don't say more than one phrase at once
                    break

    def say_now(self, phrase):
        for listener in self.speech_listeners:
            listener.on_speech(phrase)
