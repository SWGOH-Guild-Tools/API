package io.guildtools.swgraphql

import java.util.*

class Utils {
    companion object {
        val CACHE_TIME = 4

        fun isStale(updated: String): Boolean {
            val date = Date(updated.toLong())
            val diff = Date().time - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            return hours > CACHE_TIME
        }
    }
}