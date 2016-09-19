#!/bin/bash

tarfile=~/result/jawiki-$(date +'%Y%m%d-%H%M').tar

cd ~

tar -cf $tarfile -C ~/config config.groovy
for c in $(cat ~/data/category.list); do
    tar -rf $tarfile "data/${c}"
done

xz -vv -3 $tarfile

