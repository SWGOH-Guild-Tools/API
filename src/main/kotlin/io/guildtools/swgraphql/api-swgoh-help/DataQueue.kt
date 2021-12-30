package io.guildtools.swgraphql.`api-swgoh-help`

enum class QUERY_TYPE{
    GUILD,
    PLAYER
}

enum class PRIORITY {
    HIGHEST,
    PATREON,
    NORMAL
}

data class QueueObject(val type: QUERY_TYPE, val allyCodes: List<Int>, val priority: PRIORITY)