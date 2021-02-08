#!/bin/bash

# Host name or IP of RPI to the host
PI=rpi

# Build server app
pushd web
yarn build
popd

# Copy navcomputer to RPI
ssh pi@${PI} 'mkdir -p ottopi'
ssh pi@${PI} 'mkdir -p ottopi/web'
scp  -r navcomputer pi@${PI}:ottopi/
scp  -r web/build   pi@${PI}:ottopi/web
