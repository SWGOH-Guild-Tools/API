package io.guildtools.swgraphql.`api-swgoh-help`
import graphql.GraphQLException

class AlreadyInQueueException constructor(position: Int, total: Int): GraphQLException("Item already in queue ${position}/${total}") {
    override fun fillInStackTrace(): Throwable {
        return this
    }
}