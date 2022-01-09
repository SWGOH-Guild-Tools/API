package io.guildtools.swgraphql.`api-swgoh-help`

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import help.swgoh.api.SwgohAPI
import help.swgoh.api.SwgohAPIFilter
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.data.Bindings
import io.guildtools.swgraphql.data.PRIORITY
import io.guildtools.swgraphql.data.QUERY_TYPE
import io.guildtools.swgraphql.data.QueueObject
import io.guildtools.swgraphql.model.types.Guild
import io.guildtools.swgraphql.model.types.Player
import io.guildtools.swgraphql.model.types.Roster
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import kotlin.concurrent.thread

class ApiWorker {
    private var _started = false
    private var _crinoloStarted = false
    private lateinit var _conn: SwgohAPI

    val crinoloApiUrl = "https://swgoh-stat-calc.glitch.me/api"

    private lateinit var _playerRepo: PlayerRepository

    private lateinit var _guildRepo: GuildRepository

    fun start() {
        if(!_started) {
            _conn = SWGOHConnection.getSession()

            // if we don't have the repos, stall starting until we do
            _playerRepo = DBConnection.getPlayerRepo() ?: return
            _guildRepo = DBConnection.getGuildRepo() ?: return

            _started = true
            thread(start = true) {
                while(SWGOHConnection.queueHasItems()) {
                    val item = SWGOHConnection.dequeue()
                    if(item[0].type == QUERY_TYPE.GUILD) {
                        try {
                            processGuild(item)
                        } catch (e: Exception) { println("Guild not found: ${item[0].key}") }
                    } else if (item[0].type == QUERY_TYPE.PLAYER) {
                        try {
                            processPlayer(item)
                        } catch (e: Exception) { println("Player finding exception") }
                    }
                }
                _started = false
            }

        }
    }

    fun startCrinolo() {
        if(!_crinoloStarted) {
            _crinoloStarted = true
            thread(start = true) {
                while (SWGOHConnection.crinoloHasItems()) {
                    val item = SWGOHConnection.dequeueCrinolo()
                    val players = calcUnitStats(item.key, item.value) ?: continue // Maybe some logging here to try and figure out if this happened?
                    savePlayers(players)
                }
                _crinoloStarted = false
            }
        }
    }

    private fun processGuild(data: MutableList<QueueObject<Int>>) {
        val json = _conn.getGuild(data[0].key).get()
        val mapper = ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val guildData = mapper.readValue(json, Array<Guild>::class.java)[0]
        _guildRepo.save(guildData)
        val players = mutableListOf<Int>()
        guildData.roster?.forEach {
            it?.allyCode?.let { allyCode -> players.add(allyCode) }
        }

        if (players.isNotEmpty()) {
            SWGOHConnection.enqueue(players, PRIORITY.HIGHEST, QUERY_TYPE.PLAYER)
        }
    }

    private fun processPlayer(data: MutableList<QueueObject<Int>>) {
        // Extract the allycodes from the list of queue objects
        val allyCodes = mutableListOf<Int>()
        data.forEach { allyCodes.add(it.key) }

        // Fetch the data from the api
        val json = _conn.getPlayers(allyCodes, playerFilter).get()

        // Enqueue the response to get the unit stats calculated
        SWGOHConnection.enqueueCrinoloRequest(data, json)
    }

    private fun calcUnitStats(queueObjects: MutableList<QueueObject<Int>>, json: String): Array<Player>? {
        val headers = org.springframework.http.HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity<String>(json, headers)
        val result = RestTemplate().postForObject(crinoloApiUrl, request, Array<Player>::class.java) ?: return null

        // Ensure we have all the required players
        // If any were missed, retry
        queueObjects.forEach { req ->
            val found = result.filter { x -> x.allyCode == req.key }
            if(found.isNotEmpty()) {
                req.retryCount++
                SWGOHConnection.requeue(req)
            }
        }

        // Grab additional information
        // This will increase the amount of data stored in the db, but will make requests faster
        // In the future when we have a better idea of how this works at scale, this may be moved to grabbing on request
        for(i in result.indices) {
            result[i] = setUnitDetails(result[i])
        }
        return result
    }

    private fun setUnitDetails(player: Player): Player {
        val newRoster = mutableListOf<Roster>()
        player.roster?.forEach { character ->
            if(character != null) {
                val data = character.defId?.let { Bindings.getCharacter(it) }
                if(data != null) {
                    val newCharacter = character.copy(
                        name = data.name,
                        alignment = data.alignment,
                        texture = data.texture,
                        categories = data.categories,
                        role = data.role
                    )
                    newRoster.add(newCharacter)
                }
            }
        }
        return player.copy(roster = newRoster)
    }

    private fun savePlayers(players: Array<Player>) {
        _playerRepo.saveAll(players.toMutableList())
    }

    companion object {
        val playerFilter: SwgohAPIFilter = SwgohAPIFilter("allyCode")
            .and("id")
            .and("name")
            .and("level")
            .and("guildRefId")
            .and("roster")
            .and("updated")
    }
}