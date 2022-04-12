import csv


class CsvWriter:
    def __init__(self, file_name):
        fieldnames = ['utc', 'twd', 'tws', 'twa', 'sog', 'cog', 'sow', 'hdg', 'awa', 'aws', 'lat', 'lon']
        self.file_name = file_name
        self.csv_file = open(file_name, 'wt', newline='')
        self.writer = csv.DictWriter(self.csv_file, fieldnames=fieldnames)
        self.writer.writeheader()

    def on_instr_data(self, instr_data):
        if instr_data.twa is not None and instr_data.hdg is not None:
            twd = (instr_data.twa + instr_data.hdg + 360) % 360
        else:
            twd = None

        self.writer.writerow({
            'utc': instr_data.utc,
            'twd': twd,
            'tws': instr_data.tws,
            'twa': instr_data.twa,
            'sog': instr_data.sog,
            'cog': instr_data.cog,
            'sow': instr_data.sow,
            'hdg': instr_data.hdg,
            'awa': instr_data.awa,
            'aws': instr_data.aws,
            'lat': instr_data.lat,
            'lon': instr_data.lon,
        })

    def close(self):
        self.csv_file.close()
        print('Created ' + self.file_name)
