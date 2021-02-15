#!/bin/bash

# Build server app
pushd web
yarn build
popd

tar cvzf update/otto-pi-update.tgz  --exclude='navcomputer/__pycache__/' navcomputer/ bt_remote/ web/build
