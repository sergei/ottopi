#!/usr/bin/env bash

#ADB="adb -s 21052587"
ADB="adb"
PYTHON=python3

SHOTS_DIR=screenshots
ANDROID_PICT_ROOT=/storage/emulated/0/Android/data/com.santacruzinstruments.ottopi/cache/
LOCAL_PICT_ROOT=~/debuglog/

rm -rf  ~/debuglog/screenshots

$ADB pull ${ANDROID_PICT_ROOT}/${SHOTS_DIR} ${LOCAL_PICT_ROOT}
$ADB shell rm -rf ${ANDROID_PICT_ROOT}/${SHOTS_DIR}

${PYTHON} screens.py ${LOCAL_PICT_ROOT}/${SHOTS_DIR}/screenshots.json
pandoc -o  ${LOCAL_PICT_ROOT}/${SHOTS_DIR}/ottop.html ${LOCAL_PICT_ROOT}/${SHOTS_DIR}/screenshots.md
pandoc -o  ${LOCAL_PICT_ROOT}/${SHOTS_DIR}/ottop.docx ${LOCAL_PICT_ROOT}/${SHOTS_DIR}/ottop.html
