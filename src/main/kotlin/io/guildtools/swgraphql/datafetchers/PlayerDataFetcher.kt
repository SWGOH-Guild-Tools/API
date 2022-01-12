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
import io.guildtools.swgraphql.data.Bindings
import io.guildtools.swgraphql.data.PRIORITY
import io.guildtools.swgraphql.data.QUERY_TYPE
import io.guildtools.swgraphql.model.types.Crew
import io.guildtools.swgraphql.model.types.Guild
import io.guildtools.swgraphql.model.types.Player
import io.guildtools.swgraphql.model.types.Roster
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class PlayerDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    private lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun player(allyCode: Int): Player {
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

        val newRoster = player.roster?.sortedByDescending { it?.gp }?.toMutableList()

        var gls = mutableListOf<Roster>()

        Bindings.getGls().forEach { gl ->
            val unit = newRoster?.find { x -> x?.defId == gl }
            if(unit != null) {
                newRoster.remove(unit)
                gls.add(unit)
            }
        }

        if(gls.isNotEmpty()) {
            gls = gls.sortedBy { it.gp }.toMutableList()
            gls.forEach { newRoster?.add(0, it) }
        }

        return player.copy(roster = newRoster)
    }

    @DgsData(parentType = "Player")
    fun roster(dfe: DgsDataFetchingEnvironment): List<Roster?> {
        val x = dfe.arguments
        val src = dfe.getSource<Player>()

        // If we have no roster, return an empty list
        if(src.roster == null) return emptyList()

        // If we don't have any arguments, we don't need to do any filtering
        if(x.isEmpty()) return src.roster

        val combatType = x["combatType"] as Int
        val roster = src.roster.filter { it?.combatType == combatType }

        // If we're only dealing with characters, return it
        if(combatType == 1) return roster

        val newRoster = mutableListOf<Roster>()
        // Else we're dealing with ships... Let's resolve the crew
        roster.forEach {
            var copy = it?.copy()
            if(copy?.combatType == 2) {
                // Try and find the characters
                val newCrew = mutableListOf<Crew>()
                copy.crew?.forEach { member ->
                    val unit = src.roster.find { x -> x?.defId == member?.unitId }
                    member?.copy(unit = unit)?.let { it1 -> newCrew.add(it1) }
                }
                copy = copy.copy(crew = newCrew)
            }
            copy?.let { it1 -> newRoster.add(it1) }
        }
        return newRoster
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

    @DgsData(field = "roster", parentType = "Guild")
    fun playerRoster(dfe: DgsDataFetchingEnvironment): List<Player>? {
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