package io.guildtools.swgraphql.data

class KeyValuePair<K,V> {
    var key: K
    var value: V

    constructor(key: K, value: V) {
        this.key = key
        this.value = value
    }
}