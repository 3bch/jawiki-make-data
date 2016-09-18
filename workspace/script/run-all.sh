#!/bin/bash

source ~/.sdkman/bin/sdkman-init.sh

set -x && \
    "$(dirname $0)/01-download/download-data.sh" && \
    groovy "$(dirname $0)/02-clear-redis/clearRedis.groovy" && \
    groovy "$(dirname $0)/03-read-page/readPage.groovy" && \
    groovy "$(dirname $0)/04-read-link/readLink.groovy" && \
    groovy "$(dirname $0)/05-search-category/searchCategory.groovy" && \
    groovy "$(dirname $0)/06-read-article/readArticle.groovy" && \
    "$(dirname $0)/07-archive/archive.sh"

