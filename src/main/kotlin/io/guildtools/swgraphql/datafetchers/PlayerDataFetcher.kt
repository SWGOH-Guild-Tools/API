package io.guildtools.swgraphql.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import graphql.GraphQLException
import io.guildtools.swgraphql.Utils
import io.guildtools.swgraphql.`api-swgoh-help`.DBConnection
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.data.PRIORITY
import io.guildtools.swgraphql.data.QUERY_TYPE
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

        val newRoster = player.roster?.sortedByDescending {
            it?.gp
        }

        return player.copy(roster = newRoster)

    }

    @DgsData(parentType = "Player")
    fun guildName(dfe: DgsDataFetchingEnvironment): String? {
        val src = dfe.getSource<Player>()
        if(src.guildRefId == null) throw GraphQLException("Failed to resolve guild name")
        val res = _guildRepo.findNameByGuildRef(src.guildRefId)
        if (res != null) {
            return res.name
        }
        return null
    }

    @DgsData(parentType = "Guild")
    fun roster(dfe: DgsDataFetchingEnvironment): List<Player>? {
        val src = dfe.getSource<Guild>()
        val id = src.id ?: return emptyList()
        val needUpdating = mutableListOf<Int>()

        // check to see if any players need updating
        val players = (_playerRepo.findPlayersByGuildRefId(id) ?: throw CacheMissException()).toMutableList()
        players.forEachIndexed { index, player ->
            if(player.updated?.let { Utils.isStale(it) } == true) {
                player.allyCode?.let { needUpdating.add(it) }
                players[index] = player.copy(isStale = true)
            }
        }

        if(needUpdating.isNotEmpty()) {
            SWGOHConnection.enqueue(needUpdating, PRIORITY.NORMAL, QUERY_TYPE.PLAYER)
        }

        // If the players is larger than the maximum allowed for the guild in-game, update all players
        // This will happen when a member is in the database, then leaves the guild.
        // Theoretically this 'outside' player will be stale, and will be updated
        // But just in case
        if(players.size > 50) {
            // Update all players
            val codes = mutableListOf<Int>()
            players.forEach { it.allyCode?.let { it1 -> codes.add(it1) } }
            SWGOHConnection.enqueue(codes, PRIORITY.HIGHEST, QUERY_TYPE.PLAYER)
        }
        return players.toList()
    }
}