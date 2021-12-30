package io.guildtools.swgraphql.datafetchers

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import help.swgoh.api.SwgohAPIFilter
import io.guildtools.swgraphql.`api-swgoh-help`.DBConnection
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.model.types.Guild
import io.guildtools.swgraphql.model.types.Player
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

@DgsComponent
class PlayerDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    private lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun Player(allyCode: Int): Player {
        try {
            DBConnection.getGuildRepo()
        } catch (e: Exception) { DBConnection.setGuildRepo(_guildRepo) }

        try {
            DBConnection.getPlayerRepo()
        } catch (e: Exception) { DBConnection.setPlayerRepo(_playerRepo) }

        val json = SWGOHConnection.getSession().getPlayer(allyCode, filter).get()
        return ObjectMapper().readValue(json, Array<Player>::class.java)[0]
    }

    @DgsData(parentType = "Guild", field = "roster")
    fun roster(dfe: DgsDataFetchingEnvironment) {

    }

    companion object {
        val filter: SwgohAPIFilter = SwgohAPIFilter("allyCode")
            .and("id")
            .and("name")
            .and("level")
            .and("guildRefId")
            .and("roster")
            .and("1640688309000")
            .and("updated")
    }
}