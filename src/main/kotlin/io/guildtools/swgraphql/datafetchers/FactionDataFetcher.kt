package io.guildtools.swgraphql.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.data.Bindings
import org.springframework.beans.factory.annotation.Autowired
import io.guildtools.swgraphql.`api-swgoh-help`.DBConnection
import io.guildtools.swgraphql.model.types.Player

@DgsComponent
class FactionDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun faction(guildRefId: String, faction: String): List<Player>? {
        DBConnection.setRepos(_guildRepo, _playerRepo)

        return _playerRepo.findByFactionAndGuildRefId(guildRefId, faction)
    }

    @DgsQuery
    fun factions(): List<String> {
        return Bindings.getFactions()
    }
}