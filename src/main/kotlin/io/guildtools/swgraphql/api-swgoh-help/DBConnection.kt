package io.guildtools.swgraphql.`api-swgoh-help`

import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import org.springframework.beans.factory.annotation.Autowired

class DBConnection private constructor(){

    @Autowired
    private lateinit var _playerRepository: PlayerRepository

    @Autowired
    private lateinit var _guildRepository: GuildRepository

    private object HOLDER {
        val INSTANCE = DBConnection()
    }
    companion object {
        private val instance: DBConnection by lazy { DBConnection.HOLDER.INSTANCE }

        fun setPlayerRepo(repository: PlayerRepository) {
            instance._playerRepository = repository
        }

        fun setGuildRepo(repository: GuildRepository) {
            instance._guildRepository = repository
        }

        fun getPlayerRepo(): PlayerRepository {
            return instance._playerRepository
        }

        fun getGuildRepo(): GuildRepository {
            return instance._guildRepository
        }
    }
}