#!/usr/bin/env bash

TARGET=supersense-tagger.jar
set -eux

rm -rf bin
mkdir -p bin

javac -classpath lib/commons-logging.jar:lib/jwnl.jar:lib/junit-3.8.2.jar:lib/stanford-postagger-2009-12-24.jar:. -d bin src/edu/cmu/ark/*.java src/edu/cmu/ark/*.java

cd bin
jar xf ../lib/commons-logging.jar 
jar xf ../lib/jwnl.jar 
jar xf ../lib/junit-3.8.2.jar 
#jar xf ../lib/stanford-postagger-2009-12-24.jar
jar cf $TARGET *
cd ..

mv bin/$TARGET .