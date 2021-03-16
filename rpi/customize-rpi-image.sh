DISK=2021-01-11-raspios-buster-armhf-lite
IMAGE_DIR=/tmp
DISK_IMAGE=${IMAGE_DIR}/${DISK}.img
DISK_ZIP=../os-image/${DISK}.zip

PI_LINUX_DIR=/mnt/raspberry/linux
PI_BOOT_DIR=/mnt/raspberry/boot

unzip -o ${DISK_ZIP} -d ${IMAGE_DIR}

# Setup loop back device
LO_DEVICE=$(sudo losetup -f -P --show ${DISK_IMAGE}) || exit
sudo fdisk -lu "${LO_DEVICE}p1"
sudo fdisk -lu "${LO_DEVICE}p2"

# Mount partitions
sudo mkdir -p ${PI_BOOT_DIR}
sudo mkdir -p ${PI_LINUX_DIR}

sudo mount "${LO_DEVICE}p1" -o rw ${PI_BOOT_DIR} || exit
sudo mount "${LO_DEVICE}p2" -o rw ${PI_LINUX_DIR} || exit

sudo cp /usr/bin/qemu-aarch64-static ${PI_LINUX_DIR}/usr/bin
sudo cp -a boot/. ${PI_BOOT_DIR}/
mkdir ${PI_LINUX_DIR}/tmp/etc/
sudo cp -a linux/etc/. ${PI_LINUX_DIR}/tmp/etc/
sudo cp install.sh ${PI_LINUX_DIR}/tmp

# Execute install script under chroot
cd ${PI_LINUX_DIR} || exit
sudo chroot . bin/bash -c "/tmp/install.sh"

sync

sudo umount -l ${PI_BOOT_DIR}
sudo umount -l ${PI_LINUX_DIR}

# Delete loop back device
sudo losetup -d "${LO_DEVICE}"

zip -j ${IMAGE_DIR}/${DISK}-otto-pi.zip ${DISK_IMAGE}

