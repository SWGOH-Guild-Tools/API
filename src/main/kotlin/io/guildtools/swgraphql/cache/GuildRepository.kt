package io.guildtools.swgraphql.cache

import io.guildtools.swgraphql.model.types.Guild
import org.springframework.data.mongodb.repository.MongoRepository

interface GuildRepository: MongoRepository<Guild, String> {
    fun findByRosterAllyCode(allyCode: Int): Guild?
}