package com.dellemc.logstream;
public class Constants {
    protected static final String DEFAULT_SCOPE = "introspectionScope";
    protected static final String DEFAULT_STREAM_NAME = "introspectionStream";
    protected static final String DEFAULT_CONTROLLER_URI = "tcp://" + System.getenv().getOrDefault("PRAVEGA_CONTROLLER_API", "pravega-controller-api.k8s-hosted.svc.cluster.local"+ ":9090";

    protected static final String DEFAULT_ROUTING_KEY = "introspectionRoutingKey";
    protected static final String DEFAULT_MESSAGE = "{\"key\":\"value\"}";

}
