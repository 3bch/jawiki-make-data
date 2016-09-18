#!/bin/sh

cd ~/source

if [ ! -f jawiki-latest-page.sql ]; then
    wget https://dumps.wikimedia.org/jawiki/latest/jawiki-latest-page.sql.gz && \
        gzip -d jawiki-latest-page.sql.gz
else
    echo Skip page.sql
fi

if [ ! -f jawiki-latest-categorylinks.sql ]; then
    wget https://dumps.wikimedia.org/jawiki/latest/jawiki-latest-categorylinks.sql.gz && \
        gzip -d jawiki-latest-categorylinks.sql.gz
else
    echo Skip categorylinks.sql
fi

if [ ! -f jawiki-latest-pages-articles.xml ]; then
    wget https://dumps.wikimedia.org/jawiki/latest/jawiki-latest-pages-articles.xml.bz2 && \
        bzip2 -d jawiki-latest-pages-articles.xml.bz2
else
    echo Skip articles.xml
fi

