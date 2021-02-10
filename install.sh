#!/bin/bash

# Host name or IP of RPI to the host
PI=rpi

# Stop existing service

ssh pi@${PI} 'sudo systemctl stop ottopi.service'
ssh pi@${PI} 'sudo systemctl disable ottopi.service'

# Build server app
pushd web
yarn build
popd

# Copy navcomputer to RPI
ssh pi@${PI} 'mkdir -p ottopi'
ssh pi@${PI} 'mkdir -p ottopi/web'
scp  -r navcomputer pi@${PI}:ottopi/
scp  -r web/build   pi@${PI}:ottopi/web
scp  -r ottopi.service   pi@${PI}:ottopi/

# Install required Python packages

ssh pi@${PI} 'sudo pip3 install -r ottopi/navcomputer/requirements.txt'

# Start service

ssh pi@${PI} 'sudo cp ottopi/ottopi.service /etc/systemd/system/ottopi.service'
ssh pi@${PI} 'sudo systemctl enable ottopi.service'
ssh pi@${PI} 'sudo systemctl start ottopi.service'

ssh pi@${PI} 'systemctl status ottopi'