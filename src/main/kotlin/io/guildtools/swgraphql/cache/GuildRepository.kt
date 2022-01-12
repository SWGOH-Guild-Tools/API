package io.guildtools.swgraphql.cache

import io.guildtools.swgraphql.model.types.Guild
import io.guildtools.swgraphql.model.types.Player
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository

interface GuildRepository: MongoRepository<Guild, String> {
    fun findByRosterAllyCode(allyCode: Int): Guild?
    @Aggregation(
        "{\'\$match\': { '_id': '?0' } }",
        "{\"\$project\": {\"name\": 1 } }"
    )
    fun findNameByGuildRef(guildRef: String): Guild?
}