package io.guildtools.swgraphql.`api-swgoh-help`

import help.swgoh.api.SwgohAPI
import help.swgoh.api.SwgohAPIBuilder
import io.guildtools.swgraphql.data.KeyValuePair
import io.guildtools.swgraphql.data.PRIORITY
import io.guildtools.swgraphql.data.QUERY_TYPE
import io.guildtools.swgraphql.data.QueueObject
import java.util.*
import java.util.concurrent.atomic.AtomicInteger



class SWGOHConnection private constructor()  {

    // Dummy holder class so that the value is passed by ref and not value

    private val api = SwgohAPIBuilder()
        .withUsername("m1ke")
        .withPassword("18lML1ZOxixG")
        .build()

    private val _crinoloQueue = LinkedList<KeyValuePair<MutableList<QueueObject<Int>>, String>>()

    private val _highestQueue = LinkedList<QueueObject<Int>>()
    private val _patreonQueue = LinkedList<QueueObject<Int>>()
    private val _normalQueue = LinkedList<QueueObject<Int>>()
    private val _lowestQueue = LinkedList<QueueObject<Int>>()

    private val _apiWorker = ApiWorker()

    private object HOLDER {
        val INSTANCE = SWGOHConnection()
    }

    companion object {
        private val instance: SWGOHConnection by lazy { HOLDER.INSTANCE }

        fun logQueueSize() {
            var size = 0
            size += instance._highestQueue.size
            size += instance._patreonQueue.size
            size += instance._normalQueue.size
            size += instance._lowestQueue.size
            println("Queue Size ${size}")
        }
        fun getSession(): SwgohAPI {
            return instance.api
        }

        fun enqueueCrinoloRequest(requests: MutableList<QueueObject<Int>> ,json: String) {
            instance._crinoloQueue.add(KeyValuePair(requests, json))
            instance._apiWorker.startCrinolo()
        }

        fun crinoloHasItems(): Boolean {
            return instance._crinoloQueue.isNotEmpty()
        }

        fun dequeueCrinolo(): KeyValuePair<MutableList<QueueObject<Int>>, String> {
            return instance._crinoloQueue.remove()
        }

        fun queueHasItems(): Boolean {
            if(instance._highestQueue.isNotEmpty()) {
                return true
            }
            if(instance._patreonQueue.isNotEmpty()) {
                return true
            }
            if(instance._normalQueue.isNotEmpty()) {
                return true
            }
            if(instance._lowestQueue.isNotEmpty()) {
                return true
            }
            return false
        }

        fun enqueue(allyCodes: List<Int>, priority: PRIORITY, requestTYPE: QUERY_TYPE) {
            allyCodes.forEach { code -> enqueue(code, priority, requestTYPE) }
        }

        fun enqueue(code: Int, priority: PRIORITY, queryType: QUERY_TYPE, count: Int = 0) {
            when(priority) {
                PRIORITY.HIGHEST -> {
                    instance._highestQueue.add(QueueObject(code, queryType, count))
                }
                PRIORITY.PATREON -> {
                    instance._patreonQueue.add(QueueObject(code, queryType, count))
                }
                PRIORITY.NORMAL -> {
                    instance._normalQueue.add(QueueObject(code, queryType, count))
                }
                PRIORITY.LOWEST -> {
                    instance._lowestQueue.add(QueueObject(code, queryType, count))
                }
            }
            instance._apiWorker.start()
        }

        fun dequeue(): MutableList<QueueObject<Int>> {
            val numToFetch = AtomicInteger(25)
            val list = mutableListOf<QueueObject<Int>>()
            val holder = Holder(QUERY_TYPE.UNKNOWN)
            dequeueLinkedList(instance._highestQueue, numToFetch, list, holder)
            dequeueLinkedList(instance._patreonQueue, numToFetch, list, holder)
            dequeueLinkedList(instance._normalQueue, numToFetch, list, holder)
            dequeueLinkedList(instance._lowestQueue, numToFetch, list, holder)
            return list
        }

        fun requeue(item: QueueObject<Int>) {
            if(item.retryCount < 5) instance._highestQueue.add(item)
            else if(item.retryCount == 5) instance._normalQueue.add(item)
            else if(item.retryCount == 6) instance._lowestQueue.add(item)
        }

        private fun dequeueLinkedList(queue: LinkedList<QueueObject<Int>>, leftToFetch: AtomicInteger, list: MutableList<QueueObject<Int>>, holder: Holder) {

            // Return if we already have enough players, or one guild
            // We can request up to 25 players, or one guild
            if(leftToFetch.get() == 0) return
            if(holder.queryType == QUERY_TYPE.GUILD) return
            if(queue.size == 0) return // Don't bother doing anything if we don't have anything in the queue

            if(instance._lowestQueue == queue) {
                // If we're the lowest queue, sleep the thread
                // We only reach this after quite a few retries, so on this 'hack' will allow for swgoh.help to catch-up
                // when we're at low load
                println("Sleeping")
                Thread.sleep(500)
                println("Waking")
            }

            try {
                for (i in 0..queue.size) {
                    // If we don't know the type, set it
                    if(holder.queryType == QUERY_TYPE.UNKNOWN) {
                        val item = queue.removeAt(i)
                        // First time round, add the key
                        list.add(item)
                        // If it's unknown and the object is guild, process the guild
                        if(item.type == QUERY_TYPE.GUILD) {
                            holder.queryType = QUERY_TYPE.GUILD
                            return
                        }

                        // If it's unknown and the object is player, add the player to the list and find 24 more
                        holder.queryType = QUERY_TYPE.PLAYER
                        leftToFetch.getAndDecrement()
                        continue
                    }

                    // We do already have a type, and it must be PLAYER
                    if(leftToFetch.get() == 0) return
                    if(queue[i].type == QUERY_TYPE.PLAYER) {
                        val item = queue.removeAt(i)
                        list.add(item)
                        leftToFetch.getAndDecrement()
                    }
                }
            } catch (e: IndexOutOfBoundsException) {} // We're modifying the list as we go along, so if we hit this then we're don anyways
        }
    }
}