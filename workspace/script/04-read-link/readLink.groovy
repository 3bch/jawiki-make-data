@Grab('redis.clients:jedis:2.8.1')
import redis.clients.jedis.*

def sqlPath = "${System.env.HOME}/source/jawiki-latest-categorylinks.sql"

def configPath = "${System.env.HOME}/config/config.groovy"
def config = new ConfigSlurper().parse(new File(configPath).getText('UTF-8'))

def jedis = new Jedis('redis')
def finishedKey = 'jawiki:readLink:finished'

if (jedis.exists(finishedKey) && config.cache.use04) {
    println 'Skip readLink'
    return
}

jedis.del(finishedKey)

new File(sqlPath).withReader('UTF-8') { reader ->

    def links = 0

    for (def line = reader.readLine(); line != null; line = reader.readLine()) {
        if (!line.startsWith('INSERT INTO')) {
            continue
        }

        def rowsStr = line.split(' VALUES ', 2)[1]
        def rows = rowsStr =~ /(?<=,|^)\([^\(\)]+\)(?=,|$)/
        rows.each { row ->
            def match = row =~ /(?<=[\(,])(-?[\d\.]+|(?<!\\)'(?:[^']|\\')*(?<![^\\]\\)'|NULL)/

            if (match.size() != 7) {
                println row
            }
            assert match.size() == 7

            def id = match[0][1]
            def category = match[1][1][1..-2]
            def type = match[6][1][1..-2]

            if (type == 'file') {
                return
            }

            def categoryPageId = jedis.get("jawiki:ns:14:title:${category}:id")
            if (categoryPageId == null) {
                return
            }

            jedis.rpush("jawiki:category:id:${categoryPageId}:pages", id)
        }

        links += rows.size()
        println "Read Links: $links"
    }
}

jedis.set(finishedKey, 'YES')

