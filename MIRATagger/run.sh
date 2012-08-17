#!/usr/bin/env bash

#demo script
#Takes sentences on stdin, tokenizes and POS tags them with the Stanford POS tagger,
#and prints the tagged output on stdout

#Should be run from the main directory for the SST code.
#Can be run elsewhere but the tagger.properties file would need to include absolute paths.

DIRNAME=`dirname $0`

java -Xmx500m -cp $DIRNAME/supersense-tagger.jar:$DIRNAME/lib/stanford-postagger-2009-12-24.jar \
 edu/cmu/ark/DiscriminativeTagger --properties $DIRNAME/tagger.properties \
 --load $DIRNAME/models/superSenseModelAllSemcor.ser.gz


