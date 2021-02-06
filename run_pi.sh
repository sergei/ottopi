#!/bin/bash

# Host name or IP of RPI to the host
PI=rpi

# Copy navcomputer to RPI
ssh pi@${PI} 'mkdir -p ottopi'
ssh pi@${PI} 'mkdir -p ottopi/web'
scp  -r navcomputer pi@${PI}:ottopi/
scp  -r web/build   pi@${PI}:ottopi/web

# Run the app
ssh pi@${PI} 'python3 ottopi/navcomputer/main.py'
