package io.guildtools.swgraphql.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
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

@DgsComponent
class PlayerDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    private lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun Player(allyCode: Int): Player {
        DBConnection.setRepos(_guildRepo, _playerRepo)

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

    @DgsData(parentType = "Guild")
    fun roster(dfe: DgsDataFetchingEnvironment): List<Player>? {
        val src = dfe.getSource<Guild>()
        val id = src.id ?: return emptyList()
        val needUpdating = mutableListOf<Int>()

        // check to see if any players need updating
        val players = (_playerRepo.findPlayersByGuildRefId(id) ?: return emptyList()).toMutableList()
        players.forEachIndexed { index, player ->
            if(player.updated?.let { Utils.isStale(it) } == true) {
                player.allyCode?.let { needUpdating.add(it) }
                players[index] = player.copy(isStale = true)
            }
        }

        if(needUpdating.isNotEmpty()) {
            SWGOHConnection.enqueue(needUpdating, PRIORITY.NORMAL, QUERY_TYPE.PLAYER)
        }
        return players.toList()
    }
}