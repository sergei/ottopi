import time

from fusion import Fusion
from raw_sensors import RawSensors
from time_diff import time_diff


def main(csv_log_name=None):
    raw = RawSensors()
    fusion = Fusion(timediff=time_diff)

    csv_file = open(csv_log_name, 'wt') if csv_log_name is not None else None
    if csv_file is not None:
        csv_file.write('t,ax,ay,az,gx,gy,gz,mx,my,mz,heading,pitch,roll\n')
    while True:
        try:
            accel, gyro, mag, t = raw.get_raw_data()
            fusion.update(accel, gyro, mag, t)
            print('heading {:.3f}, pitch {:.3f}, roll {:.3f}'.format(fusion.heading, fusion.pitch, fusion.roll))
            if csv_file is not None:
                csv_file.write('{},{},{},{},{},{},{},{},{},{},{},{},{}\n'.format(t,
                                                                                 accel[0], accel[1], accel[2],
                                                                                 gyro[0], gyro[1], gyro[2],
                                                                                 mag[0], mag[1], mag[2],
                                                                                 fusion.heading, fusion.pitch,
                                                                                 fusion.roll
                                                                                 ))
        except OSError as e:
            print('Failed to read sensors {}'.format(e))
        time.sleep(0.01)


if __name__ == '__main__':
    main('sensors.csv')
