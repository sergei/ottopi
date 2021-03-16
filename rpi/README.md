The image is done by cloning the image running on first OttoPi box:

```
sudo dd if=/dev/sdb of=~github/ottopi/os-image/YYYY-MM-DD-ottopi.img
```

Once it's cloned we shrink it 
```
./pishrink.sh ../os-image/YYYY-MM-DD-ottopi.img
```

And zip it after that 
