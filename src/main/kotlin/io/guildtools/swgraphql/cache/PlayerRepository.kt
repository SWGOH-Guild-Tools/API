package io.guildtools.swgraphql.cache

import io.guildtools.swgraphql.model.types.Player
import org.springframework.data.mongodb.repository.MongoRepository

interface PlayerRepository: MongoRepository<Player, String> {
    fun findPlayerByAllyCode(allyCode: Int): Player?
}