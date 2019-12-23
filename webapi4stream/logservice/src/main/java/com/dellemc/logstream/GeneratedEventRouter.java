package com.dellemc.logstream;

import io.pravega.connectors.flink.PravegaEventRouter;

public class GeneratedEventRouter implements PravegaEventRouter<GeneratedEvent> {
    @Override
    public String getRoutingKey(GeneratedEvent generatedEvent) {
        return generatedEvent.key;
    }
}

