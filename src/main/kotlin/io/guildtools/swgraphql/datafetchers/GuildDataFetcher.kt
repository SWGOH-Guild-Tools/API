package io.guildtools.swgraphql.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import io.guildtools.swgraphql.Utils
import io.guildtools.swgraphql.`api-swgoh-help`.DBConnection
import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.guildtools.swgraphql.cache.GuildRepository
import io.guildtools.swgraphql.cache.PlayerRepository
import io.guildtools.swgraphql.data.PRIORITY
import io.guildtools.swgraphql.data.QUERY_TYPE
import io.guildtools.swgraphql.model.types.Guild
import org.springframework.beans.factory.annotation.Autowired


@DgsComponent
class GuildDataFetcher {

    @Autowired
    lateinit var _guildRepo: GuildRepository

    @Autowired
    lateinit var _playerRepo: PlayerRepository

    @DgsQuery
    fun Guild(allyCode: Int): Guild {
        DBConnection.setRepos(_guildRepo, _playerRepo)

        var guild = _guildRepo.findByRosterAllyCode(allyCode)

        if(guild?.updated == null) {
            SWGOHConnection.enqueue(mutableListOf(allyCode), PRIORITY.NORMAL, QUERY_TYPE.GUILD)
            throw CacheMissException()
        }

        if(Utils.isStale(guild.updated!!)) {
            SWGOHConnection.enqueue(allyCode, PRIORITY.NORMAL, QUERY_TYPE.GUILD)
            guild = guild.copy(isStale = true)
        }

        return guild
    }
}