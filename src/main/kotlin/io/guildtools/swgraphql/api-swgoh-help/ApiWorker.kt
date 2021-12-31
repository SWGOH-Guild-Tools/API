package io.guildtools.swgraphql.`api-swgoh-help`

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import help.swgoh.api.SwgohAPI
import help.swgoh.api.SwgohAPIFilter
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.model.types.Guild
import io.guildtools.swgraphql.model.types.Player
import org.springframework.beans.factory.annotation.Autowired
import kotlin.concurrent.thread

class ApiWorker {
    private var _started = false
    private lateinit var _conn: SwgohAPI

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
                val queue = SWGOHConnection.getQueue()
                while(!queue.isEmpty()) {
                    val item = queue.poll()
                    if(item.type == QUERY_TYPE.GUILD) {
                        processGuild(item)
                    } else if (item.type == QUERY_TYPE.PLAYER) {
                        processPlayer(item)
                    }
                }
                _started = false
            }

        }
    }

    private fun processGuild(data: QueueObject) {
        println("Fetching guild data...")
        val json = _conn.getGuild(data.allyCodes[0]).get()
        val mapper = ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val guildData = mapper.readValue(json, Array<Guild>::class.java)[0]
        println("Saving guild data...")
        _guildRepo.save(guildData)
        val players = mutableListOf<Int>()
        guildData.roster?.forEach {
            if (it != null) {
                it.allyCode?.let { it1 -> players.add(it1) }
            }
        }

        if (players != null && players.isNotEmpty()) {
            SWGOHConnection.enqueue(players, PRIORITY.HIGHEST, QUERY_TYPE.PLAYER)
        }
    }

    private fun processPlayer(data: QueueObject) {
        println("Fetching player data...")
        val json = _conn.getPlayers(data.allyCodes, playerFilter).get()
        val playerData = ObjectMapper().readValue(json, Array<Player>::class.java)
        _playerRepo.saveAll(playerData.toMutableList())
        val missingCodes = mutableListOf<Int>()
        data.allyCodes.forEach {
            val found = playerData.filter { x -> x.allyCode == it }
            if(found.isNotEmpty()) {
                missingCodes.add(it)
            }
        }

        if(missingCodes.isNotEmpty()) {
            SWGOHConnection.enqueue(missingCodes, PRIORITY.HIGHEST, QUERY_TYPE.PLAYER)
        }
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