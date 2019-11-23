package com.dellemc.pravega.app;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.examples.java.wordcount.util.WordCountData;
import org.apache.flink.api.java.DataSet;
import io.pravega.client.ClientConfig;
import io.pravega.client.ClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.client.stream.impl.JavaSerializer;

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditApp {
    private static final Logger logger = LoggerFactory.getLogger(AuditApp.class);
    private static final int READER_TIMEOUT_MS = 2000;
    private static final int LISTENER_PORT = 63333;
    private static final int NUM_LINES = 20;

    public static void write2(String scope, String streamName, URI controllerURI, String routingKey, List<String> messages) {
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
                logger.info(String.format("Writing message: '%s' with routing-key: '%s' to stream '%s / %s'%n",
                        message, routingKey, scope, streamName));
                final CompletableFuture writeFuture = writer.writeEvent(routingKey, message);
            });
        }
    }

    public static void main(String argv[]) throws Exception {
	final ParameterTool params = ParameterTool.fromArgs(argv);
	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
        env.enableCheckpointing(1000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        DataSet<String> text; 
	test();
	// force the app to run longer on the cluster
        for (int i = 0; i < 3600000; i++) {
		System.err.println(String.format("Printing counter=%s", i));
		Thread.sleep(500); 
        }
    }
    public static void test() throws Exception {
        List<String> allMessages;
        try {
            allMessages = new ArrayList<String>();
            allMessages.add(Constants.DEFAULT_MESSAGE);
	    write2(Constants.DEFAULT_SCOPE, Constants.DEFAULT_STREAM_NAME, URI.create(Constants.DEFAULT_CONTROLLER_URI), Constants.DEFAULT_ROUTING_KEY, allMessages);
            logger.info(String.format("Writing DEFAULT_MESSAGE=%s", Constants.DEFAULT_MESSAGE));
	} catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
