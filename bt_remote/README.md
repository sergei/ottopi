# Notes on BT on PI 
### Command line utilities 
```buildoutcfg

sudo bluetoothctl 

[bluetooth]# scan on
Discovery started
[NEW] Device C7:A3:E8:B2:B1:EB PRC-1

[bluetooth]# pair C7:A3:E8:B2:B1:EB
Attempting to pair with C7:A3:E8:B2:B1:EB
[CHG] Device C7:A3:E8:B2:B1:EB Connected: yes
[CHG] Device C7:A3:E8:B2:B1:EB ServicesResolved: yes
[CHG] Device C7:A3:E8:B2:B1:EB Paired: yes

[PRC-1]# info C7:A3:E8:B2:B1:EB
Device C7:A3:E8:B2:B1:EB (public)
	Name: PRC-1
	Alias: PRC-1
	Appearance: 0x03c1
	Icon: input-keyboard
	Paired: yes
	Trusted: no
	Blocked: no
	Connected: yes
	LegacyPairing: no
	UUID: Human Interface Device    (00001812-0000-1000-8000-00805f9b34fb)
	RSSI: -68
```

# How to enable bluetoothd debug output
### Edit bluetooth.service 
```
sudo nano /etc/systemd/system/bluetooth.target.wants/bluetooth.service
ExecStart=/usr/lib/bluetooth/bluetoothd -d
```
### Restart daemon
```
sudo systemctl daemon-reload
sudo systemctl restart bluetooth
```
### Watch output 
````
journalctl --unit=bluetooth -f
````
