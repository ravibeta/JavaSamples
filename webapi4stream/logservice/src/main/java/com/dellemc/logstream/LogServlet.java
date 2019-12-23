package com.dellemc.logstream;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.*;
import io.pravega.client.ClientConfig;
import io.pravega.client.ClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.client.stream.impl.JavaSerializer;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.connectors.flink.PravegaWriterMode;
import io.pravega.connectors.flink.serialization.PravegaSerialization;
import io.pravega.connectors.flink.watermark.LowerBoundAssigner;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "LogServlet", urlPatterns = {"log"}, loadOnStartup = 1) 
public class LogServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LogServlet.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.getWriter().print("Log Service Available!");  
    }

    public static void write(String scope, String streamName, URI controllerURI, String routingKey, List<String> messages) {
        // StreamManager streamManager = StreamManager.create(controllerURI);
        ClientConfig clientConfig = ClientConfig.builder().controllerURI(controllerURI).credentials(new DefaultCredentials("password", "desdp")).build();
        StreamManager streamManager = StreamManager.create(clientConfig);

        final boolean scopeIsNew = streamManager.createScope(scope);

        StreamConfiguration streamConfig = StreamConfiguration.builder()
              .scalingPolicy(ScalingPolicy.fixed(1))
              .build();
        final boolean streamIsNew = streamManager.createStream(scope, streamName, streamConfig);

        try (ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
            EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName,
                                                                              new JavaSerializer<String>(),
                                                                              EventWriterConfig.builder().build())) {
            messages.forEach(message -> {
                logger.info(String.format("Writing message: '%s' with routing-key: '%s' to stream '%s / %s'%n",
                        message, routingKey, scope, streamName));
                final CompletableFuture writeFuture = writer.writeEvent(routingKey, message);
              logger.info("Wrote message!");
            });
        }
        if (streamManager != null) streamManager.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String data = null;
        try {
                for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null && entry.getValue().length >= 1) {
                                data = entry.getKey();
                                break;
                        }
                }
                if (data == null) {
                        data = request.getParameter("data");
                }
                if (data != null){
                        List<String> allMessages = new ArrayList<>();
                        String[] lines = data.split("\n");
                        for (String line : lines) {
                        System.err.println(line);
                        allMessages.add(line);
                        if (allMessages.size() > Constants.NUM_LINES) {
                                write(Constants.DEFAULT_SCOPE, Constants.DEFAULT_STREAM_NAME, URI.create(Constants.DEFAULT_CONTROLLER_URI),
                                        Constants.DEFAULT_ROUTING_KEY, allMessages);
                                allMessages = new ArrayList<>();
                        }
                        }
                        if (allMessages.size() > 0) {
                                write(Constants.DEFAULT_SCOPE, Constants.DEFAULT_STREAM_NAME, URI.create(Constants.DEFAULT_CONTROLLER_URI),
                                        Constants.DEFAULT_ROUTING_KEY, allMessages);
                                allMessages = new ArrayList<>();
                        }
                }
        } catch (Exception e) {
                logger.error("Exception:", e);
        }
        response.getWriter().print("Success");
    }
}
