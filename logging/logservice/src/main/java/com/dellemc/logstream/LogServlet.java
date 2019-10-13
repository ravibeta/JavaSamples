package com.dellemc.logstream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import io.pravega.client.ClientConfig;
import io.pravega.client.ClientFactory;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventRead;
import io.pravega.client.stream.EventStreamReader;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ReaderConfig;
import io.pravega.client.stream.ReaderGroupConfig;
import io.pravega.client.stream.ReinitializationRequiredException;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.impl.JavaSerializer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@SuppressWarnings("deprecation")
@WebServlet(name = "LogServlet", urlPatterns = {"log"}, loadOnStartup = 1) 
public class LogServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LogServlet.class);
    private static final int READER_TIMEOUT_MS = 2000;
    private static final int LISTENER_PORT = 63333;
    private static final int NUM_LINES = 20;

public static void write(String scope, String streamName, URI controllerURI, String routingKey, List<String> messages) {
        StreamManager streamManager = StreamManager.create(controllerURI);
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
               // logger.debug("Writing message: '{}' with routing-key: '{}' to stream '{} / {}'%n",
               //     message, routingKey, scope, streamName);
               final CompletableFuture writeFuture = writer.writeEvent(routingKey, message);
            });
        }
}

public static List<String> read(String scope, String streamName, URI controllerURI) {
        List<String> allTransformedMessages = new ArrayList<>();
        StreamManager streamManager = StreamManager.create(controllerURI);

        final boolean scopeIsNew = streamManager.createScope(scope);
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(1))
                .build();
        final boolean streamIsNew = streamManager.createStream(scope, streamName, streamConfig);

        final String readerGroup = UUID.randomUUID().toString().replace("-", "");
        final ReaderGroupConfig readerGroupConfig = ReaderGroupConfig.builder()
                .stream(Stream.of(scope, streamName))
                .build();
        try (ReaderGroupManager readerGroupManager = ReaderGroupManager.withScope(scope, controllerURI)) {
            readerGroupManager.createReaderGroup(readerGroup, readerGroupConfig);
        }

        try (ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
             EventStreamReader<String> reader = clientFactory.createReader("reader",
                                                                           readerGroup,
                                                                           new JavaSerializer<String>(),
                                                                           ReaderConfig.builder().build())) {
            logger.info("Reading all the events from {}/{}", scope, streamName);
            EventRead<String> event = null;
            do {
                try {
                    event = reader.readNextEvent(READER_TIMEOUT_MS);
                    if (event.getEvent() != null) {
                        // logger.debug("Read event '{}'%n", event.getEvent());
                        String message = event.getEvent().toString();
                        List<String> messages = new ArrayList<>();
                        messages.add(message);
                        write("srsScope", "srsStream", URI.create("tcp://10.247.142.138:9090"), "srsRoutingKey", messages);
                        allTransformedMessages.add(message);
                    }
                } catch (ReinitializationRequiredException e) {
                    //There are certain circumstances where the reader needs to be reinitialized
                    e.printStackTrace();
                }
            } while (event.getEvent() != null);
            System.out.format("No more events from %s/%s%n", scope, streamName);
        }
        return allTransformedMessages;
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.getWriter().print("Log Service Available!");  
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
		    	if (allMessages.size() > NUM_LINES) {
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
		logger.error(e);
	}
        response.getWriter().print("Success");
    }
}
