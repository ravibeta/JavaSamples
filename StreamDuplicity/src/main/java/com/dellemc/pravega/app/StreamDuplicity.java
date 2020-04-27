package com.dellemc.pravega.app;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import io.pravega.common.Exceptions;
import io.pravega.client.ClientConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.connectors.flink.PravegaWriterMode;
import io.pravega.connectors.flink.PravegaEventRouter;
import io.pravega.connectors.flink.serialization.PravegaSerialization;
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
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.collector.selector.OutputSelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.IterativeStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.TimestampExtractor;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.function.Function;


public class StreamDuplicity {
    private static final Logger logger = LoggerFactory.getLogger(StreamDuplicity.class);
    private static EventStreamClientFactory clientFactory;
    private static ReaderGroup readerGroup;
    private static ReaderGroupManager readerGroupManager;
    private static int readerNum = 1;
    private static int numRetries = 0;
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

    public static void main(String argv[]) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(argv);
        readProperties();
        PravegaConfig pravegaConfig = PravegaConfig.fromParams(ParameterTool.fromArgs(argv))
           .withControllerURI(URI.create(controllerUriText))
           .withDefaultScope(scopeName)
           .withCredentials(adminCredentials())
           .withHostnameValidation(false);
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(numberOfSegments))
                .build();
        ClientConfig clientConfig = ClientConfig.builder()
            .credentials(adminCredentials())
            .controllerURI(URI.create(controllerUriText)).build();
        clientFactory = EventStreamClientFactory.withScope(scopeName, clientConfig);
        createStream(pravegaConfig, streamName, streamConfig);
        if (readerGroupManager == null) {
            readerGroupManager = ReaderGroupManager.withScope(scopeName, clientConfig);
        }
        String readerGroupName = "data-transfer-reader-group";
        makeReaderGroup(readerGroupName);
        Stream stream = pravegaConfig.resolve(streamName);
        // for debugging purpose only
        // for (int i = 0; i < 5; i++) {
        //       writeEvents(clientFactory, streamName, i);
        // }
        final Reader reader = new Reader()
                .withClientFactory(clientFactory)
                .withReaderGroup(readerGroup, readerNum, numRetries, retryMillis, restartable, segment, position)
                .withAWSCredentials(key, secret, region, bucketName)
                .withPravegaProperties(scopeName, streamName, controllerUriText, username, password, numberOfSegments)
                .withStream(streamName);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
               reader.start();
        });
        logger.info("Started reader");
        Futures.await(future);
        // System.exit(0);
    }

    private static void writeEvents(EventStreamClientFactory clientFactory, String streamName, int eventNumber) {
        EventWriterConfig eventWriterConfig = EventWriterConfig.builder()
                .transactionTimeoutTime(30_000)
                .build();
        JavaSerializer<String> SERIALIZER = new JavaSerializer<String>();
        EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName, SERIALIZER, eventWriterConfig);
        String payload = "Hello World Lady Ada Lovelace " + String.valueOf(eventNumber);
        CompletableFuture<Void> writeEvent = writer.writeEvent(UUID.randomUUID().toString(), payload);
        int sizeOfEvent = io.netty.buffer.Unpooled.wrappedBuffer(SERIALIZER.serialize(payload)).readableBytes();
        logger.info("Wrote event of size:{}", sizeOfEvent);
        Exceptions.handleInterrupted(() -> {
            try {
                writeEvent.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Not handled here.
            }
        });
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
    public static Stream createStream(PravegaConfig pravegaConfig, String streamName, StreamConfiguration streamConfig) {
        Stream stream = pravegaConfig.resolve(streamName);

        try(StreamManager streamManager = StreamManager.create(pravegaConfig.getClientConfig())) {
            streamManager.createScope(stream.getScope());
            streamManager.createStream(stream.getScope(), stream.getStreamName(), streamConfig);
        }

        return stream;
    }
    public static void readProperties() throws IOException {
        try (InputStream input = StreamDuplicity.class.getClassLoader().getResourceAsStream("config.properties")) {
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
        } catch (IOException e) {
            logger.info("configuration properties file not found: {}", e);
            throw e;
        }
    }
}
