import csv


class PhrfEntry:
    PHRF_TYPE_TOD = 'tod'
    PHRF_TYPE_TOT = 'tot'

    def __init__(self, name, phrf, phrf_type, tcf_a, tcf_b):
        self.name = name
        self.phrf = phrf
        self.phrf_type = phrf_type
        self.tcf_a = tcf_a
        self.tcf_b = tcf_b


class PhrfTable:
    def __init__(self):
        self.table = []

    def read_file(self, file_name):
        table = []
        try:
            with open(file_name, newline='') as csvfile:
                phrf_reader = csv.reader(csvfile, delimiter=',')
                next(phrf_reader)  # Skip header

                for row in phrf_reader:
                    if len(row) == 0:
                        continue

                    if len(row) < 2:
                        raise ValueError('Too few rows')
                    if len(row) < 3:
                        phrf_type = PhrfEntry.PHRF_TYPE_TOD
                    elif len(row) > 3 and len(row) != 5:
                        raise ValueError("tot must have A and B values")
                    else:
                        phrf_type = row[2].strip()
                        if phrf_type not in [PhrfEntry.PHRF_TYPE_TOD, PhrfEntry.PHRF_TYPE_TOT]:
                            raise ValueError('Unsupported PHRF type: {} '.format(phrf_type))
                        if phrf_type == PhrfEntry.PHRF_TYPE_TOT:
                            tcf_a = int(row[3])
                            tcf_b = int(row[4])
                        else:
                            tcf_a = None
                            tcf_b = None

                    entry = PhrfEntry(name=row[0], phrf=int(row[1]), phrf_type=phrf_type, tcf_a=tcf_a, tcf_b=tcf_b)
                    table.append(entry)
        except ValueError as e:
            print(e)
            return False

        self.table = table
        return True

    def get_corrected_times(self, distance, elapsed_time):
        corrected_times = {}
        for entry in self.table:
            if entry.phrf_type == PhrfEntry.PHRF_TYPE_TOD:
                time_allowance = distance * entry.phrf
                corrected_times[entry.phrf] = elapsed_time - time_allowance
            else:
                tcf = entry.tcf_a / (entry.tcf_b - entry.phrf)
                corrected_times[entry.phrf] = elapsed_time * tcf

        return corrected_times
