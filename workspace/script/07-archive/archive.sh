#!/bin/bash

tarfile=~/result/jawiki-$(date +'%Y%m%d-%H%M').tar

cd ~
tar -cf $tarfile --exclude .gitkeep data -C ~/config config.groovy && \
    xz -vv -3 $tarfile

