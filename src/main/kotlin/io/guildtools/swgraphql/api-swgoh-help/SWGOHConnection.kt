package io.guildtools.swgraphql.`api-swgoh-help`

import help.swgoh.api.SwgohAPI
import help.swgoh.api.SwgohAPIBuilder
import java.util.*

class SWGOHConnection private constructor()  {
    private val api = SwgohAPIBuilder()
        .withUsername("m1ke")
        .withPassword("18lML1ZOxixG")
        .build()

    private val _queue = PriorityQueue<QueueObject>(11, compareBy { it.priority })

    private val _apiWorker = ApiWorker()

    private object HOLDER {
        val INSTANCE = SWGOHConnection()
    }

    companion object {
        private val instance: SWGOHConnection by lazy { HOLDER.INSTANCE }
        fun getSession(): SwgohAPI {
            return instance.api
        }
        fun getQueue(): PriorityQueue<QueueObject> {
            return instance._queue
        }
        fun enqueue(allyCodes: List<Int>, priority: PRIORITY, requestTYPE: QUERY_TYPE) {
            instance._queue.add(QueueObject(requestTYPE, allyCodes, priority))
            instance._apiWorker.start()
        }
    }
}