package io.guildtools.swgraphql

import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SwGraphQlApplication {

    @Autowired
    lateinit var playerRepo: PlayerRepository

    @Autowired
    lateinit var guildRepo: GuildRepository
}

fun main(args: Array<String>) {
    runApplication<SwGraphQlApplication>(*args)
}

