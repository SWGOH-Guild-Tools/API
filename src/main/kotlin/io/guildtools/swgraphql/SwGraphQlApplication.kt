package io.guildtools.swgraphql
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.guildtools.swgraphql.data.Bindings
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.concurrent.thread

@SpringBootApplication
class SwGraphQlApplication

fun main(args: Array<String>) {
    runApplication<SwGraphQlApplication>(*args)
}

