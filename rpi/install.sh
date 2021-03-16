echo "This is the install script"
uname -a
sudo apt-get install -y python3-pip
sudo apt-get install -y espeak

# See https://www.raspberrypi.org/documentation/configuration/wireless/access-point-routed.md
# Install access point software package
sudo apt install -y hostapd
# Enable the wireless access point service and set it to start when your Raspberry Pi boots:
sudo systemctl unmask hostapd
sudo systemctl enable hostapd

# In order to provide network management services (DNS, DHCP) to wireless clients,
# the Raspberry Pi needs to have the dnsmasq software package installed:
sudo apt install -y dnsmasq

# Finally, install netfilter-persistent and its plugin iptables-persistent.
# This utilty helps by saving firewall rules and restoring them when the Raspberry Pi boots:
sudo DEBIAN_FRONTEND=noninteractive apt install -y netfilter-persistent iptables-persistent

# Enable routing
sudo iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
# save the current firewall rules
sudo netfilter-persistent save

# Copy premodified files after installation is done
cp -a  /tmp/etc/. /etc
