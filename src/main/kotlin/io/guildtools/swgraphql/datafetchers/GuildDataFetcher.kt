package io.guildtools.swgraphql.datafetchers

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import help.swgoh.api.SwgohAPIFilter
import io.guildtools.swgraphql.`api-swgoh-help`.DBConnection
import io.guildtools.swgraphql.`api-swgoh-help`.PRIORITY
import io.guildtools.swgraphql.`api-swgoh-help`.QUERY_TYPE
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.model.types.Guild
import org.springframework.beans.factory.annotation.Autowired
import java.io.File


@DgsComponent
class GuildDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun Guild(allyCode: Int): Guild {
        try {
            DBConnection.getGuildRepo()
        } catch (e: Exception) { DBConnection.setGuildRepo(_guildRepo) }

        try {
            DBConnection.getPlayerRepo()
        } catch (e: Exception) { DBConnection.setPlayerRepo(_playerRepo) }

        SWGOHConnection.enqueue(mutableListOf(allyCode), PRIORITY.NORMAL, QUERY_TYPE.GUILD)
        throw CacheMissException()
    }

    companion object {
        val filter = SwgohAPIFilter("id")
            .and("name")
            .and("desc")
            .and("members")
            .and("gp")
            .and("roster")
    }
}