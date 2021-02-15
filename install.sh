#!/bin/bash

# Host name or IP of RPI to the host
PI=rpi

# Stop existing service

ssh pi@${PI} 'sudo systemctl stop ottopi.service'
ssh pi@${PI} 'sudo systemctl disable ottopi.service'
ssh pi@${PI} 'sudo systemctl stop ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl disable ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl stop ottopi-update.service'
ssh pi@${PI} 'sudo systemctl disable ottopi-update.service'

# Build server app
pushd web
yarn build
popd

# Copy navcomputer to RPI
ssh pi@${PI} 'mkdir -p ottopi'
ssh pi@${PI} 'mkdir -p ottopi/web'

scp  -r navcomputer           pi@${PI}:ottopi/
scp  -r bt_remote             pi@${PI}:ottopi/
scp  -r web/build             pi@${PI}:ottopi/web
scp  -r update                pi@${PI}:ottopi/

scp  -r ottopi.service        pi@${PI}:ottopi/
scp  -r ottopi-bt.service     pi@${PI}:ottopi/
scp  -r ottopi-update.service pi@${PI}:ottopi/

# Install required Python packages

ssh pi@${PI} 'sudo pip3 install -r ottopi/navcomputer/requirements.txt'

# Start service

ssh pi@${PI} 'sudo cp ottopi/ottopi.service /etc/systemd/system/ottopi.service'
ssh pi@${PI} 'sudo cp ottopi/ottopi-bt.service /etc/systemd/system/ottopi-bt.service'
ssh pi@${PI} 'sudo cp ottopi/ottopi-update.service /etc/systemd/system/ottopi-update.service'
ssh pi@${PI} 'sudo systemctl enable ottopi.service'
ssh pi@${PI} 'sudo systemctl start ottopi.service'
ssh pi@${PI} 'sudo systemctl enable ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl start ottopi-bt.service'
ssh pi@${PI} 'sudo systemctl enable ottopi-update.service'
ssh pi@${PI} 'sudo systemctl start ottopi-update.service'

ssh pi@${PI} 'systemctl status ottopi'
ssh pi@${PI} 'systemctl status ottopi-bt'
ssh pi@${PI} 'systemctl status ottopi-update'
