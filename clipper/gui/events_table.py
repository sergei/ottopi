from tkinter import ttk, VERTICAL, END
from tkinter.ttk import Treeview

from gui.clip_event import ClipEvent
from gui.race_info import RaceInfo

TYPE_RACE = 'race'
TYPE_EVENT = 'event'


class EventsTable:
    def __init__(self, top, on_race_selected, on_event_selected):
        self.on_race_selected = on_race_selected
        self.on_event_selected = on_event_selected

        table_frame = ttk.Frame(top, padding="3 3 12 12")
        table_frame.grid(column=0, row=0, sticky='nwes')

        columns = ['name']
        self.events_tree = Treeview(table_frame, columns=columns, show='headings')
        self.events_tree = Treeview(table_frame)

        self.events_tree.bind('<<TreeviewSelect>>', self.item_selected)
        self.events_tree.grid(row=0, column=0, sticky='nsew')

        # add a scrollbar
        scrollbar = ttk.Scrollbar(table_frame, orient=VERTICAL, command=self.events_tree.yview)
        self.events_tree.configure(yscroll=scrollbar.set)
        scrollbar.grid(row=0, column=1, sticky='ns')

    def show_races(self, races: list[RaceInfo]):

        # Clear existing items
        for item in self.events_tree.get_children():
            self.events_tree.delete(item)

        for race in races:
            race_node = self.events_tree.insert('', END, text=race.name, open=True, tags=(race.uuid, TYPE_RACE))
            events = race.events
            for event in events:
                self.events_tree.insert(race_node, END, text=event.name, tags=(event.to_json(), TYPE_EVENT))

    def remove_event(self, event_to_remove: ClipEvent):
        for race_iid in self.events_tree.get_children():
            for event_iid in self.events_tree.get_children(race_iid):
                item = self.events_tree.item(event_iid)
                event = ClipEvent.from_json(item['tags'][0])
                if event.uuid == event_to_remove.uuid:
                    self.events_tree.delete(event_iid)
                    return

    def update_event(self, event_to_update: ClipEvent):
        for race_iid in self.events_tree.get_children():
            for event_iid in self.events_tree.get_children(race_iid):
                item = self.events_tree.item(event_iid)
                old_event = ClipEvent.from_json(item['tags'][0])
                if old_event.uuid == event_to_update.uuid:
                    self.events_tree.item(event_iid, text=event_to_update.name, tags=(event_to_update.to_json(), TYPE_EVENT))
                    return

    def item_selected(self, e):
        for selected_item in self.events_tree.selection():
            item = self.events_tree.item(selected_item)
            item_type = item['tags'][1]
            if item_type == TYPE_RACE:
                race_uuid = item['tags'][0]
                self.on_race_selected(race_uuid)
            else:
                event = ClipEvent.from_json(item['tags'][0])
                self.on_event_selected(event)
