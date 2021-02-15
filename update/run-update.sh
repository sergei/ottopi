#!/bin/bash

UPDATE_ARCHIVE=/home/pi/data/gpx/otto-pi-update.tgz

if  gunzip -t ${UPDATE_ARCHIVE} ; then
  cd /home/pi/ottopi/
  rm -rf navcomputer/
  rm -rf bt_remote/
  rm -rf web/build
  tar xvzf ${UPDATE_ARCHIVE}  --exclude='navcomputer/__pycache__/' navcomputer/ bt_remote/ web/build
  rm ${UPDATE_ARCHIVE}
else
  echo 'The update archive either not found or not valid'
fi
