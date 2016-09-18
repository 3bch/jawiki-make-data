import javax.xml.XMLConstants
import javax.xml.stream.*
import javax.xml.stream.events.*

import groovy.json.*

@Grab('redis.clients:jedis:2.8.1')
import redis.clients.jedis.*

def configPath = "${System.env.HOME}/config/config.groovy"
def config = new ConfigSlurper().parse(new File(configPath).getText('UTF-8'))

def xmlPath = "${System.env.HOME}/source/jawiki-latest-pages-articles.xml"

def jedis = new Jedis('redis')

def cacheDir = new File("${System.env.HOME}/data/")

def categories = config.data.categories.collect { name, info ->
    return [
        name: name,
        dir: new File(cacheDir, name),
        count: 0,
        has: { id ->
            return jedis.sismember("jawiki:id:${id}:category", name)
        }
    ]
}

if (config.data.includeAll) {
    categories << [
        name: '_all',
        dir: new File(cacheDir, '_all'),
        count: 0,
        has: { id -> return true }
    ]
}

if (config.cache.use06) {
    categories = categories.findAll { category ->
        def meta = new File(category.dir, 'meta.json')
        return !meta.exists()
    }
}

if (categories.isEmpty()) {
    println "Skip readArticle"
    return
}

categories.each { category ->
    category.dir.deleteDir()
    category.dir.mkdir()
}


def factory = XMLInputFactory.newInstance()
factory.setProperty('http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit', '500000000')

def wikifil(text) {
    return text
        .toLowerCase()
        .replace('&amp;', '')
        .replace('&lt;', '')
        .replace('&gt;', '')
        .replaceAll('<ref[^<]*?</ref>', '')
        .replaceAll('<gallery[^<]*?</gallery>', '')
        .replaceAll('<.*?>', '')
        .replaceAll(/\[http:.*?( |.(?=\]))/, '[')
        .replaceAll(/\[\[(image|file|ファイル):.*?\]\]/, '')
        .replaceAll(/\[\[category:(.*?)(?:\|.*?)?\]\]/, '[[$1]]')
        .replaceAll(/\[\[-a-z]*?:.*?\]\]/, '')
        .replaceAll(/\[\[(?:[^\]]*?\|)?(.*?)\]\]/, '[[$1]]')
        .replaceAll(/\{\{仮リンク\|(.*?)(?:\|.*?)?\}\}/, '$1')
        .replaceAll(/\{\{pdflink\|(.*?)\}\}/, '$1')
        .replaceAll(/\{\{[^\{\}]*?(\{\{[^\{\}]*?\}\}[^\{\}]*?)*?\}\}/, '') // 複数行の {{}} を削除
        .replaceAll(/\{\|[^\{\}]*?\|\}/, '') // 複数行の {| |} を削除
        .replaceAll(/\{[^\{\}]*?(\{[^\{\}]*?\}[^\{\}]*?)*?\}/, '') // 複数行の {} を削除
        .replace('[', '')
        .replace(']', '')
        .replace('{}', '')
        .replaceAll(/&#?[0-9a-z]+;/, ' ')
        .replaceAll(/'+/, "'")
}

def nextStart(reader, localName) {
    while (reader.hasNext()) {
        def event = reader.nextEvent()
        if (event.eventType != XMLEvent.START_ELEMENT) {
            continue
        }
        if (event.name.localPart != localName) {
            continue
        }
        return event
    }
}

def readText(reader, localName) {
    nextStart(reader, localName)
    def builder = new StringBuilder()

    while (reader.hasNext()) {
        def event = reader.nextEvent()
        if (event.eventType == XMLEvent.CHARACTERS) {
            if (!event.isWhiteSpace()) {
                builder.append(event.getData())
            }
            continue
        }
        if (event.eventType != XMLEvent.END_ELEMENT) {
            continue
        }
        if (event.name.localPart != localName) {
            continue
        }
        return builder.toString()
    }
}


new File(xmlPath).withInputStream { input ->
    def reader = factory.createXMLEventReader(input)
    def n = 0;

    while (reader.hasNext()) {
        nextStart(reader, 'page')
        def title = readText(reader, 'title')
        def ns = readText(reader, 'ns')

        if (ns != '0') {
            continue
        }

        def id = readText(reader, 'id')
        def text = readText(reader, 'text')
        def wikifilText = null

        categories.each { category ->
            if (!category.has(id)) {
                return
            }
            def idStr = id.padLeft(7, '0')

            def dir = new File(category.dir, idStr[0..2])
            dir.mkdir()

            if (wikifilText == null) {
                wikifilText = wikifil(text)
            }

            def file = new File(dir, "${idStr}.txt")
            file.withPrintWriter('UTF-8') { writer ->
                writer.println(title.replaceAll('[\r\n]', ' '))
                writer.println(wikifilText)
            }

            category.count += 1
        }

        if (n % 10000 == 0) {
            println "READ: $n"
        }

        n += 1
    }

    categories.each { category ->
        def metaFile = new File(category.dir, 'meta.json')
        def json = new JsonBuilder()
        def meta = jedis.hmget("jawiki:category:name:${category.name}:meta", 'title', 'depth')
        json(
            name: category.name,
            title: meta[0],
            depth: meta[1],
            count: category.count
        )
        metaFile.write(json.toPrettyString(), 'UTF-8')
    }

    println "FINISH: $n"
}

