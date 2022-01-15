package io.guildtools.swgraphql

import io.guildtools.swgraphql.`api-swgoh-help`.SWGOHConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Component

@Component
class Scheduler(meterRegistry: MeterRegistry) {

    init {
        val data = SWGOHConnection.getQueues()
        meterRegistry.gaugeCollectionSize("gt_queue_size", Tags.of("name", "highest"), data[0])
        meterRegistry.gaugeCollectionSize("gt_queue_size", Tags.of("name", "patreon"), data[1])
        meterRegistry.gaugeCollectionSize("gt_queue_size", Tags.of("name", "normal"), data[2])
        meterRegistry.gaugeCollectionSize("gt_queue_size", Tags.of("name", "lowest"), data[3])
    }
}