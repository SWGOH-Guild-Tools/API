package io.guildtools.swgraphql.data

import io.guildtools.swgraphql.Constants
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import org.springframework.web.client.RestTemplate

class Bindings {
    private val _map = HashMap<String, Character>()

    private val _gls = listOf("GRANDMASTERLUKE", "SITHPALPATINE", "GLREY", "SUPREMELEADERKYLOREN", "JEDIMASTERKENOBI", "CAPITALEXECUTOR")

    private val _factions: List<String>

    private constructor() {
        val factions = mutableListOf<String>()
        fetchData("characters", factions)
        fetchData("ships", factions)

        // Convert to unique set, then back to a sorted list
        _factions = factions.toSet().toList().sorted()
    }

    private fun fetchData(endpoint: String, list: MutableList<String>) {
        val data = RestTemplate().getForObject("${Constants.WEB_URL}/${endpoint}.json", Array<Character>::class.java) ?: throw Exception("Cannot obtain game data")
        data.forEach {
            for (category in it.categories) {
                list.add(category)
            }
            _map[it.base_id] = it
        }
    }

    private object HOLDER {
        val INSTANCE = Bindings()
    }

    companion object {
        private val instance: Bindings by lazy { HOLDER.INSTANCE }

        fun getCharacter(defId: String): Character? { return instance._map[defId] }

        fun getFactions(): List<String> { return instance._factions }

        fun getGls(): List<String> { return instance._gls }
    }
}