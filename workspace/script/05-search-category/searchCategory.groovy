import groovy.transform.Field

@Grab('redis.clients:jedis:2.8.1')
import redis.clients.jedis.*


@Field
def jedis = new Jedis('redis')

def configPath = "${System.env.HOME}/config/config.groovy"
def config = new ConfigSlurper().parse(new File(configPath).getText('UTF-8'))


def categories = config.data.categories.collect { name, info ->
    return [
        name: name,
        title: info.title,
        depth: info.depth ?: config.data.defaultDepth ?: 5
    ]
}


def printPage(categoryName, id, parents, pageSet, depthLimit) {
    if (depthLimit < parents.size()) {
        return
    }
    if (pageSet.contains(id)) {
        return
    }

    pageSet << id

    def page = jedis.hmget("jawiki:id:${id}:page", 'ns', 'title')
    jedis.sadd("jawiki:id:${id}:category", categoryName)

    if (pageSet.size() % 1000 == 0) {
        println "WRITE: ${pageSet.size()}"
    }


    def key = "jawiki:category:id:${id}:pages"
    def hasChild = jedis.exists(key)
    if (!hasChild) {
        return
    }

    def len = jedis.llen(key)
    def childIds = jedis.lrange(key, 0, len)
    def newParents = [page[1], *parents]
    childIds.each { childId ->
        printPage(categoryName, childId, newParents, pageSet, depthLimit)
    }
}


categories.each { category ->
    def metaKey = "jawiki:category:name:${category.name}:meta"

    if (config.cache.use05 && jedis.exists(metaKey)) {
        println "Skip search $category.name"
        return
    }

    jedis.del(metaKey)

    println "search $category.name"
    def id = jedis.get("jawiki:ns:14:title:${category.title}:id")

    def pageSet = [] as Set
    printPage(category.name, id, [], pageSet, category.depth)

    jedis.hmset(metaKey, [title: category.title, depth: category.depth as String])

    println "Finish $category.name: ${pageSet.size()}"
}

