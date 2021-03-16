Use Image 2021-01-11-raspios-buster-armhf-lite.zip to flash SD Card 

In order to be able to modify image install [qemu-user-static](https://wiki.debian.org/RaspberryPi/qemu-user-static)

Run script [customize-rpi-image.sh](customize-rpi-image.sh) to customize original RPI image

This directory contains file that need to modified on Raspberry PI SD card
It has two folders: boot and main 
- boot - is the boot partition of SD card
  - ssh - file to tell headless RPI to enable ssh at the first boot
- main - is the main partition of SD card 
