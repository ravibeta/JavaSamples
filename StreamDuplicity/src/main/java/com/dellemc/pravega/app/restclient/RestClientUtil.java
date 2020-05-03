package com.dellemc.pravega.app.restclient;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.Base64;

public class RestClientUtil {
    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    public static final int DEFAULT_READ_TIMEOUT = 30000;
    public static final int DEFAULT_MAX_ENTITY_SIZE = 1024;

    /**
     * Creates the default client used for all rest client.
     *
     * @return the client.
     */
    public static Client defaultClient() {
        ClientBuilder builder = ClientBuilder.newBuilder();
        builder.register(JacksonObjectMapperProvider.class);
        builder.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        builder.property(ClientProperties.READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
        builder.sslContext(SSLUtil.getTrustAllContext());
        builder.hostnameVerifier(SSLUtil.getNullHostnameVerifier());
        return builder.build();
    }
}
