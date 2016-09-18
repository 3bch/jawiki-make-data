@Grab('redis.clients:jedis:2.8.1')
import redis.clients.jedis.*
import redis.clients.jedis.exceptions.JedisDataException


def configFile = new File("${System.env.HOME}/config/config.groovy")
def config = new ConfigSlurper().parse(configFile.getText('UTF-8'))

if (config.cache.clear) {
    def jedis = new Jedis('redis', 6379, 1800000)
    jedis.flushDB()
}

def lastException = null

for (int i = 0; i < 10; i++) {
    try {
        def jedis = new Jedis('redis')
        jedis.ping()
        return
    } catch (JedisDataException e) {
        lastException = e
        println "Redis is unavailable: ${e.message}"
        println "Sleeping..."
        Thread.sleep(1000)
    }
}

throw new RuntimeException("Redis is unavailable", lastException)

