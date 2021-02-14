#!/bin/bash

# Host name or IP of RPI to the host
PI=rpi

# Copy navcomputer to RPI
ssh pi@${PI} 'mkdir -p ottopi'
scp  -r bt_remote pi@${PI}:ottopi/


