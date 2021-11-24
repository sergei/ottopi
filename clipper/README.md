# Clipper 

## What is this script
This script splits one large GOPRO recording into small clips 
each of them containing event of interest and then makes the movie 
using these clips. The resulting movie is much smaller than original 
GOPRO footage

## How to run this script 
There are two ways how the script can be used:
- Single pass - fully automatic 
- Two passes - allows inserting manual events and make several movies

### Running in single pass mode 
```bash
PYTHONPATH=$PYTHONPATH:../navcomputer  caffeinate -i  /usr/local/bin/python3.9 main.py -f
```

### Running in two passes mode

Run the first pass
```bash
PYTHONPATH=$PYTHONPATH:../navcomputer /usr/local/bin/python3.9 main.py
```

- Open the generated KML file in Google Earth 
- Rename the generated YAML file and open it in text editor 
- Edit YAML file based on what you see in KML file 

Run the second pass
```bash
PYTHONPATH=$PYTHONPATH:../navcomputer caffeinate -i  /usr/local/bin/python3.9 main.py movie.yaml
```

### How to prevent Mac from sleeping 

Use caffeinate

