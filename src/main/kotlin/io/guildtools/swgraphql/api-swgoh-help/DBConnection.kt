package io.guildtools.swgraphql.`api-swgoh-help`

import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository

class DBConnection private constructor(){

    private var _playerRepository: PlayerRepository? = null

    private var _guildRepository: GuildRepository? = null

    private object HOLDER {
        val INSTANCE = DBConnection()
    }
    companion object {
        private val instance: DBConnection by lazy { DBConnection.HOLDER.INSTANCE }

        fun setRepos(guildRepository: GuildRepository, playerRepository: PlayerRepository) {
            if(instance._playerRepository == null) {
                instance._playerRepository = playerRepository
            }

            if(instance._guildRepository == null) {
                instance._guildRepository = guildRepository
            }
        }

        fun getPlayerRepo(): PlayerRepository? {
            return instance._playerRepository
        }

        fun getGuildRepo(): GuildRepository? {
            return instance._guildRepository
        }
    }
}