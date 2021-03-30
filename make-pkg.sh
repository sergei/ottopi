#!/bin/bash

# Build server app
pushd web || exit
#yarn build
popd || exit

tar cvzf update/otto-pi-update.tgz  --exclude='navcomputer/__pycache__/' --exclude='data/' --exclude='*/__pycache__/' \
     *.service navcomputer/ bt_remote/ web/build imu/ sk_py_client/ update/run-update.sh

