#/bin/bash

MODEL_NAME=8388
#pocketsphinx_continuous  -lm data/model/${MODEL_NAME}.lm -dict data/model/${MODEL_NAME}.dic  -infile data/commands.raw

pocketsphinx_continuous  -jsgf data/sentences.jsgf  -dict data/model/${MODEL_NAME}.dic  -infile data/commands.raw


