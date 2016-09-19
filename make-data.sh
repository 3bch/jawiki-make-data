#!/bin/bash

make_data () {

    cd $(dirname $0)

    redis_status=$(docker-compose ps redis | awk -F'  +' 'NR == 3 {print $3}')

    echo Redis Status ["$redis_status"]
    if [ "$redis_status" != "Up" ]; then
        docker-compose up -d redis
    fi

    docker-compose stop  workspace
    docker-compose build workspace && \
        docker-compose up    workspace
}

make_data | tee log/$(date +'%Y%m%d-%H%M%S').log

