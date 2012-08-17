#!/usr/bin/env zsh
set -eux
./build.sh
rm -rf bin
rm -rf data/data-splitting
rm -rf data/gaz
rm -rf notes
rm data/SEM.BI.*
rm data/SEM_07.BI
gzip data/SEM.BI
svn status
rm -rf **/.svn

###example command sequence to wrap up a tarball
#misc/prepare.sh
#cd ../
#mv DiscriminativeTagger/ SupersenseTagger
#tar -czvf SupersenseTagger-03-08-11.tar.gz SupersenseTagger/

