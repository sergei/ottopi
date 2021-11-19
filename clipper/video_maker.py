import os

from clipper.overlay_maker import OverlayMaker


def make_video(work_dir, base_name, race_events):
    overlay_maker = OverlayMaker(work_dir, base_name, 1920, 256)
    for evt_idx, evt in enumerate(race_events):
        for epoch_idx, epoch in enumerate(evt['history']):
            file_name = f'ovl_{evt_idx:04d}_{epoch_idx:04d}.png'
            overlay_maker.add_epoch(file_name, epoch)
