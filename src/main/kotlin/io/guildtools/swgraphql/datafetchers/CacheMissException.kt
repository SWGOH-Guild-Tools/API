package io.guildtools.swgraphql.datafetchers

import graphql.GraphQLException

class CacheMissException: GraphQLException("Item not found in database... Adding to the queue")