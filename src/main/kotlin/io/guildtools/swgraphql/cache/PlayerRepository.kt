package io.guildtools.swgraphql.cache

import io.guildtools.swgraphql.model.types.Player
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository

interface PlayerRepository: MongoRepository<Player, String> {
    fun findPlayerByAllyCode(allyCode: Int): Player?
    fun findPlayersByGuildRefId(ref: String): List<Player>?

    @Aggregation(
        "{\'\$match\': { 'guildRefId': '?0' } }",
        "{\"\$project\": {\"name\": 1, \"roster\": {\$filter: {input: \"\$roster\", as: \"roster\", cond: { \$in: [\"?1\", \"\$\$roster.categories\"]} } } } }"
    )
    fun findByFactionAndGuildRefId(guildref: String, faction: String): List<Player>?
}