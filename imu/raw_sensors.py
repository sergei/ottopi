import time

# noinspection PyUnresolvedReferences
from icm20948 import ICM20948


class RawSensors:
    def __init__(self):
        self.imu = ICM20948()

    def get_raw_data(self):
        mag = self.imu.read_magnetometer_data()
        ax, ay, az, gx, gy, gz = self.imu.read_accelerometer_gyro_data()
        t = time.time()

        return (ax, ay, az), (gx, gy, gz), mag, t
