from tkinter import *
from tkinter import ttk

from raw_instr_data import RawInstrData


class InstrumentsView:
    def __init__(self, top):
        ttk.Label(top, text="COG").grid(column=0, row=0, sticky=W)
        ttk.Label(top, text="SOG").grid(column=0, row=1, sticky=W)
        ttk.Label(top, text="HDG").grid(column=2, row=0, sticky=W)
        ttk.Label(top, text="SOW").grid(column=2, row=1, sticky=W)
        ttk.Label(top, text="AWA").grid(column=4, row=0, sticky=W)
        ttk.Label(top, text="AWS").grid(column=4, row=1, sticky=W)
        ttk.Label(top, text="TWA").grid(column=6, row=0, sticky=W)
        ttk.Label(top, text="TWS").grid(column=6, row=1, sticky=W)

        self.sog = StringVar()
        self.cog = StringVar()
        self.hdg = StringVar()
        self.sow = StringVar()
        self.awa = StringVar()
        self.aws = StringVar()
        self.twa = StringVar()
        self.tws = StringVar()

        ttk.Label(top, textvariable=self.cog).grid(column=1, row=0, sticky=W)
        ttk.Label(top, textvariable=self.sog).grid(column=1, row=1, sticky=W)
        ttk.Label(top, textvariable=self.hdg).grid(column=3, row=0, sticky=W)
        ttk.Label(top, textvariable=self.sow).grid(column=3, row=1, sticky=W)
        ttk.Label(top, textvariable=self.awa).grid(column=5, row=0, sticky=W)
        ttk.Label(top, textvariable=self.aws).grid(column=5, row=1, sticky=W)
        ttk.Label(top, textvariable=self.twa).grid(column=7, row=0, sticky=W)
        ttk.Label(top, textvariable=self.tws).grid(column=7, row=1, sticky=W)

    def set_instr_data(self, instr_data: RawInstrData):
        self.sog.set(f'{instr_data.sog:.1f}') if instr_data.sog is not None else self.sog.set('---')
        self.cog.set(f'{instr_data.cog:.1f}') if instr_data.cog is not None else self.cog.set('---')
        self.hdg.set(f'{instr_data.hdg:.1f}') if instr_data.hdg is not None else self.hdg.set('---')
        self.sow.set(f'{instr_data.sow:.1f}') if instr_data.sow is not None else self.sow.set('---')
        self.awa.set(f'{instr_data.awa:.1f}') if instr_data.awa is not None else self.awa.set('---')
        self.aws.set(f'{instr_data.aws:.1f}') if instr_data.aws is not None else self.aws.set('---')
        self.twa.set(f'{instr_data.twa:.1f}') if instr_data.twa is not None else self.twa.set('---')
        self.tws.set(f'{instr_data.tws:.1f}') if instr_data.tws is not None else self.tws.set('---')

