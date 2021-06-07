sudo iptables -t nat -A POSTROUTING -o usb0 -j MASQUERADE
sudo netfilter-persistent save

