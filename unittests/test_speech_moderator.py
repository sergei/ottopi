import datetime
import unittest

from navigator_listener import NavigationListener
from speech_moderator import SpeechModerator, SpeechEntry, SpeechEntryType


class TesSpeechModerator(unittest.TestCase):

    def test_moderator(self):
        class Listener(NavigationListener):
            def __init__(self):
                super().__init__()
                self.phrase = None

            def on_speech(self, phrase: str):
                self.phrase = phrase

            def pop_phrase(self):
                phrase = self.phrase
                self.phrase = None
                return phrase

        listener = Listener()
        listeners = [listener]
        speech_moderator = SpeechModerator(listeners)

        utc = datetime.datetime(2020, 5, 17, 11, 45, 57, tzinfo=datetime.timezone.utc)
        speech_moderator.say_something(utc, False)
        self.assertIsNone(listener.pop_phrase())  # Nothing to say obviously

        utc += datetime.timedelta(0, 1)
        speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_EVENT, utc, "nav event1"))
        speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_UPDATE, utc, "nav update1"))
        speech_moderator.say_something(utc, False)
        self.assertEqual(listener.pop_phrase(), "nav event1")  # Event Should be said immediately

        # No update should be announced unit minimum time is elapsed
        utc += datetime.timedelta(0, 2)
        speech_moderator.say_something(utc, False)
        self.assertIsNone(listener.pop_phrase())

        # Update should be announced after minimum time is elapsed
        utc += datetime.timedelta(0, 30)
        speech_moderator.say_something(utc, False)
        self.assertEqual(listener.pop_phrase(), "nav update1")  # Now the update should be announced

        utc += datetime.timedelta(0, 240)
        speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_UPDATE, utc, "nav update2"))
        speech_moderator.add_entry(SpeechEntry(SpeechEntryType.DEST_UPDATE, utc, "dest update1"))
        speech_moderator.say_something(utc, False)
        self.assertEqual(listener.pop_phrase(), "dest update1")  # Destination update has higher priority

        # No update should be announced unit minimum time is elapsed
        utc += datetime.timedelta(0, 2)
        self.assertIsNone(listener.pop_phrase())

        # Update should be announced after minimum time is elapsed
        utc += datetime.timedelta(0, 30)
        speech_moderator.say_something(utc, False)
        self.assertEqual(listener.pop_phrase(), "nav update2")  # Now the update should be announced


if __name__ == '__main__':
    unittest.main()
