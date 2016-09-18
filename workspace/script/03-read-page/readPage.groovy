@Grab('redis.clients:jedis:2.8.1')
import redis.clients.jedis.*

def sqlPath = "${System.env.HOME}/source/jawiki-latest-page.sql"
def finishedKey = 'jawiki:readPage:finished'

def configPath = "${System.env.HOME}/config/config.groovy"
def config = new ConfigSlurper().parse(new File(configPath).getText('UTF-8'))

def jedis = new Jedis('redis')
if (jedis.exists(finishedKey) && config.cache.use03) {
    println "Skip readPage"
    return
}

jedis.del(finishedKey)

new File(sqlPath).withReader('UTF-8') { reader ->

    def pages = 0

    for (def line = reader.readLine(); line != null; line = reader.readLine()) {
        if (!line.startsWith('INSERT INTO')) {
            continue
        }

        def rowsStr = line.split(' VALUES ', 2)[1]
        def rows = rowsStr =~ /(?<=,|^)\([^\(\)]+\)(?=,|$)/
        rows.each { row ->
            def match = row =~ /(?<=[\(,])(-?[\d\.]+|(?<!\\)'(?:[^']|\\')*(?<![^\\]\\)'|NULL)/

            if (match.size() != 13) {
                println row
            }
            assert match.size() == 13

            def id = match[0][1]
            def ns = match[1][1]
            def title = match[2][1][1..-2]

            if (ns ==~ '0') {
                jedis.hmset("jawiki:id:${id}:page", [ns: ns, title: title])
            } else if (ns == '14') {
                jedis.set("jawiki:ns:${ns}:title:${title}:id", id)
                jedis.hmset("jawiki:id:${id}:page", [ns: ns, title: title])
            }

        }

        pages += rows.size()
        println "Read Pages: $pages"
    }
}

jedis.set(finishedKey, 'YES')

