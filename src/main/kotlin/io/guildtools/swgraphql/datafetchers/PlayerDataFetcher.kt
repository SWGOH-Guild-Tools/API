package io.guildtools.swgraphql.datafetchers

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import help.swgoh.api.SwgohAPIFilter
import io.guildtools.swgraphql.Utils
import io.guildtools.swgraphql.`api-swgoh-help`.DBConnection
import io.guildtools.swgraphql.`api-swgoh-help`.PRIORITY
import io.guildtools.swgraphql.`api-swgoh-help`.QUERY_TYPE
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.model.types.Guild
import io.guildtools.swgraphql.model.types.Player
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.io.File
import java.util.*

@DgsComponent
class PlayerDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    private lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun Player(allyCode: Int): Player {
        DBConnection.setRepos(_guildRepo, _playerRepo)

        val query = Query()
        query.addCriteria(Criteria.where("allyCode").`is`(allyCode))

        var player = _playerRepo.findPlayerByAllyCode(allyCode)

        if(player?.updated == null) {
            SWGOHConnection.enqueue(mutableListOf(allyCode), PRIORITY.NORMAL, QUERY_TYPE.PLAYER)
            throw CacheMissException()
        }

        if(Utils.isStale(player.updated!!)) {
            SWGOHConnection.enqueue(mutableListOf(allyCode), PRIORITY.NORMAL, QUERY_TYPE.PLAYER)
            player = player.copy(isStale = true)
        }

        return player

    }

    @DgsData(parentType = "Guild", field = "roster")
    fun roster(dfe: DgsDataFetchingEnvironment) {

    }
}