import threading
import time

from speaker import Speaker


class TimerTalker:
    speaker: Speaker

    def __init__(self):
        self.thread = None
        self.keep_running = None
        self.speaker = Speaker.get_instance()
        self.start_moment_idx = 0

    def start_timer(self, time_to_start):
        if self.thread is not None and self.thread.is_alive():
            print('Timer thread is already running')
            return False
        else:
            print('launching timer thread')
            self.thread = threading.Thread(target=self.__countdown, name='timer_thread', args=[time_to_start])
            self.thread.start()
            return True

    def update_timer(self, time_to_start):
        print('stopping timer thread')
        self.keep_running = False
        self.thread.join()
        print('launching timer thread')
        self.thread = threading.Thread(target=self.__countdown, name='timer_thread', args=[time_to_start])
        self.thread.start()

    def stop_timer(self):
        self.keep_running = False
        self.thread.join()

    MOMENTS = [(5 * 60, 'sounds/300-seconds.mp3'),
               (4*60 + 30, None),
               (4*60, None),
               (3*60 + 30, None),
               (3*60, None),
               (2*60 + 30, None),
               (2*60, None),
               (1*60 + 30, None),
               (60, None),
               (50, None),
               (40, None),
               (30, None),
               (21, 'sounds/21-seconds.mp3'),
               ]

    def __countdown(self, time_to_start):
        print('Timer thread started')
        self.keep_running = True
        now = time.time()
        start_at = now + time_to_start

        self.reset_time_checker(time_to_start)

        while self.keep_running:
            time.sleep(0.1)
            time_remaining = start_at - time.time()
            say_now, time_entry = self.check_time_left(time_remaining)
            if say_now:
                if time_entry[1] is None:
                    time_left = time_entry[0]
                    phrase = self.format_time(time_left)
                    self.speaker.on_speech(phrase)
                else:
                    self.speaker.play_file(time_entry[1])

            if time_remaining < 0:
                break

        print("Timer thread has finished")

    @staticmethod
    def format_time(time_to_start):
        if time_to_start >= 60:
            minutes = int(time_to_start / 60)
            seconds = int(time_to_start - minutes * 60)
            if seconds == 0:
                return '{} minute{} to start'.format(minutes, 's' if minutes > 1 else '')
            elif seconds == 30:
                return '{} and a half to start'.format(minutes)
            else:
                return '{} {}'.format(minutes, seconds)
        elif time_to_start > 10:
            return '{} seconds'.format(time_to_start)
        else:
            return '{}'.format(time_to_start)

    def reset_time_checker(self, time_remaining):
        self.start_moment_idx = 0
        for idx in range(self.start_moment_idx, len(self.MOMENTS)):
            if self.MOMENTS[idx][0] > time_remaining:
                self.start_moment_idx = idx + 1
                break

    def check_time_left(self, time_remaining):
        for idx in range(self.start_moment_idx, len(self.MOMENTS)):
            if self.MOMENTS[idx][0] > time_remaining:
                self.start_moment_idx = idx + 1
                return True, self.MOMENTS[idx]

        return False, None
