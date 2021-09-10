#!/bin/bash

# Host name or IP of RPI to the host
PI=wrpi

# Build the package
./make-pkg.sh

# Stop existing service
ssh pi@${PI} 'sudo systemctl stop ottopi.service'
ssh pi@${PI} 'sudo systemctl disable ottopi.service'
ssh pi@${PI} 'sudo systemctl stop ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl disable ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl stop ottopi-imu.service'
ssh pi@${PI} 'sudo systemctl disable ottopi-imu.service'
ssh pi@${PI} 'sudo systemctl stop ottopi-rtk.service'
ssh pi@${PI} 'sudo systemctl disable ottopi-rtk.service'
ssh pi@${PI} 'sudo systemctl stop ottopi-update.service'
ssh pi@${PI} 'sudo systemctl disable ottopi-update.service'

# Copy package to RPI
scp update/otto-pi-update.tgz pi@${PI}:/home/pi/data/gpx/otto-pi-update.tgz
scp update/run-update.sh pi@${PI}:ottopi/update/run-update.sh

# Execute the update script
ssh pi@${PI} 'ottopi/update/run-update.sh'

# Install required Python packages (not part of update script, since update is done without internet connection)
ssh pi@${PI} 'sudo pip3 install -r ottopi/navcomputer/requirements.txt'

# Start services
ssh pi@${PI} 'sudo cp ottopi/ottopi.service /etc/systemd/system/ottopi.service'
ssh pi@${PI} 'sudo cp ottopi/ottopi-bt.service /etc/systemd/system/ottopi-bt.service'
ssh pi@${PI} 'sudo cp ottopi/ottopi-imu.service /etc/systemd/system/ottopi-imu.service'
ssh pi@${PI} 'sudo cp ottopi/ottopi-rtk.service /etc/systemd/system/ottopi-rtk.service'
ssh pi@${PI} 'sudo cp ottopi/ottopi-update.service /etc/systemd/system/ottopi-update.service'
ssh pi@${PI} 'sudo systemctl enable ottopi.service'
ssh pi@${PI} 'sudo systemctl start ottopi.service'
ssh pi@${PI} 'sudo systemctl enable ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl start ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl enable ottopi-imu.service'
ssh pi@${PI} 'sudo systemctl start ottopi-imu.service'
ssh pi@${PI} 'sudo systemctl enable ottopi-rtk.service'
ssh pi@${PI} 'sudo systemctl start ottopi-rtk.service'
ssh pi@${PI} 'sudo systemctl enable ottopi-update.service'
ssh pi@${PI} 'sudo systemctl start ottopi-update.service'

# Show services status
ssh pi@${PI} 'systemctl status ottopi'
ssh pi@${PI} 'systemctl status ottopi-bt'
ssh pi@${PI} 'systemctl status ottopi-imu'
ssh pi@${PI} 'systemctl status ottopi-rtk'
ssh pi@${PI} 'systemctl status ottopi-update'
