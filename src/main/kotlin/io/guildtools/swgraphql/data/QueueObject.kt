package io.guildtools.swgraphql.data

data class QueueObject<T>(val key: T, var type: QUERY_TYPE, var retryCount: Int)
