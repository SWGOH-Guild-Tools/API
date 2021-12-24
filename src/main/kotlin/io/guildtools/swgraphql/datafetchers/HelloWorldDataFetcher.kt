package io.guildtools.swgraphql.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import io.guildtools.swgraphql.model.types.Show

@DgsComponent
class HelloWorldDataFetcher {
    @DgsQuery
    fun helloWorld(): Show {
        return Show("Star Wars Episode 3", 2005)
    }
}