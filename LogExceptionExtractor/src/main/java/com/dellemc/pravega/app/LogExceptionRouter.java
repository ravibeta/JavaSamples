package com.dellemc.pravega.app;

import io.pravega.connectors.flink.PravegaEventRouter;

public class LogExceptionRouter implements PravegaEventRouter<LogException> {
    @Override
    public String getRoutingKey(LogException logException) {
        return logException.timestamp;
    }
}

