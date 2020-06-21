package com.dellemc.pravega.app;

import com.google.common.base.Strings;
import io.pravega.common.Exceptions;
import io.pravega.common.concurrent.Futures;
import io.pravega.client.ClientConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.Checkpoint;
import io.pravega.client.stream.EventRead;
import io.pravega.client.stream.EventStreamReader;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.Position;
import io.pravega.client.stream.ReaderConfig;
import io.pravega.client.stream.ReaderGroup;
import io.pravega.client.stream.ReaderGroupConfig;
import io.pravega.client.stream.ReinitializationRequiredException;
import io.pravega.client.stream.StreamCut;
import io.pravega.client.stream.impl.Credentials;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.client.segment.impl.Segment;
import io.pravega.client.stream.impl.StreamCutImpl;
import io.pravega.client.stream.TruncatedDataException;
import io.pravega.client.stream.impl.JavaSerializer;
import io.pravega.common.concurrent.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.function.Function;
import static io.pravega.common.concurrent.ExecutorServiceHelpers.newScheduledThreadPool;


public class StreamDuplicity {
    private static final Logger logger = LoggerFactory.getLogger(StreamDuplicity.class);
    private static final String CONFIG_PROPERTIES_PATH = "/data/config/config.properties";
    private static EventStreamClientFactory clientFactory;
    private static ReaderGroup readerGroup;
    private static ReaderGroupManager readerGroupManager;
    private static int readerNum = 1;
    private static int numRetries = 1;
    private static int retryMillis = 100; 
    private static boolean restartable = false;
    private static long segment = 0L;
    private static long position = 0L;
    private static String bucketName;
    private static String key;
    private static String secret;
    private static String region;
    private static String scopeName;
    private static String streamName;
    private static String controllerUriText;
    private static String username;
    private static String password;
    private static Integer numberOfSegments;
    private static Boolean standalone;
    private static String mode;
    private static String prefix;
    private static long sequence = 0L;

    private static MessageClient messageClient = new MessageClient();
    private static ScheduledExecutorService backgroundExecutor = newScheduledThreadPool(1, String.format("Restarter"));

    public static void main(String argv[]) throws Exception {
        readProperties();
        if (standalone == true) {
            run();
            System.exit(0);
        }
        logger.info("Starting daemon mode");
        try {
            Supplier<Boolean> restartable = () -> false;
            CompletableFuture loop = Futures.loop(restartable, () -> {
                logger.info("Reader task run started");
                try {
                  CompletableFuture future = Futures.delayedTask(() -> {
                    try {
                        if (messageClient.getRestartReader()) {
                            logger.info("reloading configuration and restarting reader");
                            run();
                        }
                    } catch(Exception e) {
                        logger.error("Reader exception: {}", e);
                    }
                    logger.info("Reader task finished run");
                    return null;
                  }, Duration.ofMillis(10000), backgroundExecutor);
                  Futures.await(future);
               } catch (Exception e) {
                    logger.error("reader task interrupted: {}", e);
               }
               logger.info("Reader task run completed");
               return null;
            }, backgroundExecutor);
          Futures.await(loop);
        } catch (Exception e) {
            logger.error("Exception:{}", e);
            System.exit(0);
        }
    }


    private static void run() throws Exception {
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(numberOfSegments))
                .build();
        ClientConfig clientConfig = ClientConfig.builder()
            .credentials(adminCredentials())
            .controllerURI(URI.create(controllerUriText)).build();
        clientFactory = EventStreamClientFactory.withScope(scopeName, clientConfig);

        if (readerGroupManager == null) {
            readerGroupManager = ReaderGroupManager.withScope(scopeName, clientConfig);
        }
        if (mode.toUpperCase().equals("READER")) {
            if (readerGroupManager == null) {
                readerGroupManager = ReaderGroupManager.withScope(scopeName, clientConfig);
            }
            String readerGroupName = "data-transfer-reader-group";
            makeReaderGroup(readerGroupName);
            final Reader reader = new Reader()
                .withClientFactory(clientFactory)
                .withReaderGroup(readerGroup, readerNum, numRetries, retryMillis, false, segment, position)
                .withAWSCredentials(key, secret, region, bucketName)
                .withPravegaProperties(scopeName, streamName, controllerUriText, username, password, numberOfSegments)
                .withStream(streamName);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                   reader.start();
            });
            logger.info("Started reader");
            Futures.await(future);
        }
        else if (mode.toUpperCase().equals("WRITER")) {
            try(StreamManager streamManager = StreamManager.create(URI.create(controllerUriText))) {
                 streamManager.createScope(scopeName);
                 streamManager.createStream(scopeName, streamName.replace("-copy","") + "-copy", streamConfig);
            } catch (Exception e) {
               log.error("Exception in creating destination stream:", e);
            }
            final Writer writer = new Writer()
                .withClientFactory(clientFactory)
                .withPrefix(prefix)
                .withWriterSequence(readerNum, numRetries, retryMillis, false, segment, position, sequence)
                .withAWSCredentials(key, secret, region, bucketName)
                .withPravegaProperties(scopeName, streamName, controllerUriText, username, password, numberOfSegments)
                .withStream(streamName)
                .withPrefix(prefix);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                   writer.prepare();
                   writer.start();
            });
            logger.info("Started writer");
            Futures.await(future);
        }
        else {
             throw new IllegalArgumentException("This tool can work only as a reader or writer. Please specify a mode of operation");
        }
        System.exit(0);
    }

    private static void makeReaderGroup(String readerGroupName) {
        if (readerGroup == null) {
            readerGroupManager.createReaderGroup(readerGroupName, ReaderGroupConfig.builder()
                    .stream(scopeName + "/" + streamName).build());
            readerGroup = readerGroupManager.getReaderGroup(readerGroupName);
            logger.info("created readerGroup {}", readerGroup.getGroupName());
        } else {
            logger.info("using existing readerGroup {}", readerGroup.getGroupName());
        }
    }

    public static DefaultCredentials adminCredentials() {
        return new DefaultCredentials(password, username);
    }

    private static StreamManager streamManager() {
        return StreamManager.create(ClientConfig.builder()
            .credentials(adminCredentials())
            .controllerURI(URI.create(controllerUriText))
            .build());
    }

    // public static Stream createStream(PravegaConfig pravegaConfig, String streamName, StreamConfiguration streamConfig) {
    //     Stream stream = pravegaConfig.resolve(streamName);
    // 
    //     try(StreamManager streamManager = StreamManager.create(pravegaConfig.getClientConfig())) {
    //         streamManager.createScope(stream.getScope());
    //         streamManager.createStream(stream.getScope(), stream.getStreamName(), streamConfig);
    //     }
    // 
    //     return stream;
    // }

    // private static void writeEvents(EventStreamClientFactory clientFactory, String streamName, int eventNumber) {
    //     EventWriterConfig eventWriterConfig = EventWriterConfig.builder()
    //             .transactionTimeoutTime(30_000)
    //             .build();
    //     JavaSerializer<String> SERIALIZER = new JavaSerializer<String>();
    //     EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName, SERIALIZER, eventWriterConfig);
    //     String payload = "Hello World Lady Ada Lovelace " + String.valueOf(eventNumber);
    //     CompletableFuture<Void> writeEvent = writer.writeEvent(UUID.randomUUID().toString(), payload);
    //     int sizeOfEvent = io.netty.buffer.Unpooled.wrappedBuffer(SERIALIZER.serialize(payload)).readableBytes();
    //     logger.info("Wrote event of size:{}", sizeOfEvent);
    //     Exceptions.handleInterrupted(() -> {
    //         try {
    //            writeEvent.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    //         } catch (Exception e) {
    //             // Not handled here.
    //        }
    //    });
    // }

    public static void readProperties() throws IOException {
        try (InputStream input =  new FileInputStream(CONFIG_PROPERTIES_PATH)) {
            Properties prop = new Properties();
            if (input == null) {
                throw new RuntimeException("Without config properties, the source and destination for this data transfer are not known.");
            } 
            prop.load(input);
            bucketName = System.getenv().getOrDefault("BUCKET_NAME", prop.getProperty("bucket_name"));
            key = System.getenv().getOrDefault("AWS_ACCESS_KEY_ID", prop.getProperty("aws_access_key_id"));
            secret = System.getenv().getOrDefault("AWS_SECRET_ACCESS_KEY", prop.getProperty("aws_secret_access_key"));
            region = System.getenv().getOrDefault("AWS_REGION", prop.getProperty("aws_region"));
            scopeName = System.getenv().getOrDefault("SCOPE_NAME", prop.getProperty("scope_name"));
            streamName = System.getenv().getOrDefault("STREAM_NAME", prop.getProperty("stream_name"));
            controllerUriText = System.getenv().getOrDefault("CONTROLLER_URI", prop.getProperty("controller_uri"));
            username = System.getenv().getOrDefault("USERNAME", prop.getProperty("pravega_username"));
            password = System.getenv().getOrDefault("PASSWORD", prop.getProperty("pravega_password"));
            numberOfSegments = Integer.valueOf(System.getenv().getOrDefault("NUMBER_OF_SEGMENTS", prop.getProperty("number_of_segments")));
            standalone = Boolean.valueOf(System.getenv().getOrDefault("STANDALONE", prop.getProperty("standalone")));
            mode = System.getenv().getOrDefault("MODE", prop.getProperty("mode"));
            logger.info("bucketName:{}, region:{}, scopeName:{}, streamName:{}, controllerUriText: {}, username:{}, password: {}, numberOfSegments: {}, standalone: {}, mode:{}", bucketName, region, scopeName, streamName, controllerUriText, username, password, String.valueOf(numberOfSegments), String.valueOf(standalone), mode);
            if (Strings.isNullOrEmpty(key)) throw new IllegalArgumentException("Please specify proper aws_access_key_id");
            if (Strings.isNullOrEmpty(secret)) throw new IllegalArgumentException("Please specify proper aws_secret_access_key");
            if (Strings.isNullOrEmpty(region)) throw new IllegalArgumentException("Please specify proper aws_region such as us-east-1 or us-east-2");
            if (Strings.isNullOrEmpty(scopeName)) throw new IllegalArgumentException("Please specify existing scope_name from stream store");
            if (Strings.isNullOrEmpty(streamName)) throw new IllegalArgumentException("Please specify existing stream_name from stream store");
            if (Strings.isNullOrEmpty(controllerUriText)) throw new IllegalArgumentException("Please specify existing controller_uri from stream store");
            if (Strings.isNullOrEmpty(username)) throw new IllegalArgumentException("Please specify existing admin credentials from stream store");
            if (Strings.isNullOrEmpty(password)) throw new IllegalArgumentException("Please specify existing admin credentials from stream store");
            if (Strings.isNullOrEmpty(mode)) throw new IllegalArgumentException("Please specify mode of operation as reader or writer");
            if (mode.toUpperCase().equals("WRITER")) {
                prefix = System.getenv().getOrDefault("PREFIX", prop.getProperty("prefix"));
                sequence = Long.valueOf(System.getenv().getOrDefault("OBJECT_SEQUENCE", prop.getProperty("object_sequence")));
                if (Strings.isNullOrEmpty(prefix)) {
                   throw new IllegalArgumentException("Please specify existing object prefix from the source S3 bucket");
                }
                if (sequence < 0) sequence = 0;
                logger.info("prefix={},sequence={}", prefix, sequence);
            }
            if (numberOfSegments < 1) numberOfSegments = 1;
        } catch (IOException e) {
            logger.info("configuration properties file not found: {}", e);
            throw e;
        }
    }
}
