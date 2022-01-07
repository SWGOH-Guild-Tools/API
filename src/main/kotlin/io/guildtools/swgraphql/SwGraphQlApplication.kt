package io.guildtools.swgraphql
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.concurrent.thread

@SpringBootApplication
class SwGraphQlApplication

fun main(args: Array<String>) {
    runApplication<SwGraphQlApplication>(*args)

    // Start a thread that
    thread(start = true) {
        while(true) {
            SWGOHConnection.logQueueSize()
            Thread.sleep(100)
        }
    }
}

