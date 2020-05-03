package com.dellemc.pravega.app;

import com.dellemc.pravega.app.restclient.RestClient;
import com.dellemc.pravega.app.restclient.RestClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageClient {
    private static final String BASE_ENDPOINT = "/v1/duplicity";

    private static final Logger LOG = LoggerFactory.getLogger(MessageClient.class);
    private RestClient restClient;

    public MessageClient() {
        String apiHost = System.getenv("API_HOST");
        if (apiHost == null) {
            LOG.error("Environment API_HOST cannot be null.");
            return;
        }

        restClient = new RestClient(RestClientUtil.defaultClient().target(apiHost + BASE_ENDPOINT));
    }

    public boolean getRestartReader() {
       try {
           if (restClient.get("/restart/reader", String.class).equals("true")) {
               return true;
           }
       } catch (Exception e) {
           LOG.info("Exception in getting restart for reader: {}", e);
       }
       return false;
    }

    public boolean getRestartWriter() {
       try {
           if (restClient.get("/restart/writer", String.class).equals("true")) {
               return true;
           }
       } catch (Exception e) {
           LOG.info("Exception in getting restart for writer: {}", e);
       }
       return false;
    }
}
