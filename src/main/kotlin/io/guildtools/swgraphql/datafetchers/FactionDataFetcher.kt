package io.guildtools.swgraphql.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.data.Bindings
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class FactionDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    lateinit var _playerRepo: PlayerRepository

//    @DgsQuery
//    fun faction(guildRefId: Int, faction: String): List<Player> {
//        DBConnection.setRepos(_guildRepo, _playerRepo)
//
//
//    }

    @DgsQuery
    fun factions(): List<String> {
        return Bindings.getFactions()
    }
}