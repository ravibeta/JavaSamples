package com.dellemc.pravega.app;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
import io.pravega.client.stream.Checkpoint;
import io.pravega.client.stream.EventRead;
import io.pravega.client.stream.EventStreamReader;
import io.pravega.client.stream.Position;
import io.pravega.client.stream.ReaderConfig;
import io.pravega.client.stream.ReaderGroup;
import io.pravega.client.stream.ReaderGroupConfig;
import io.pravega.client.stream.ReinitializationRequiredException;
import io.pravega.client.stream.StreamCut;
import io.pravega.client.segment.impl.Segment;
import io.pravega.client.stream.impl.StreamCutImpl;
import io.pravega.client.segment.impl.SegmentTruncatedException;
import io.pravega.client.stream.TruncatedDataException;
import io.pravega.client.stream.impl.ByteBufferSerializer;
import io.pravega.client.stream.impl.JavaSerializer;
import io.pravega.client.stream.Serializer;
import io.pravega.common.util.ByteArraySegment;
import io.pravega.common.util.ByteBufferUtils;
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
import java.io.Serializable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.function.Function;

import static io.pravega.common.concurrent.ExecutorServiceHelpers.newScheduledThreadPool;

public class Reader {
    private static final Logger logger = LoggerFactory.getLogger(StreamDuplicity.class);

    private final ByteBufferSerializer SERIALIZER = new ByteBufferSerializer();

    private String readerId;
    private EventStreamClientFactory clientFactory;
    private EventStreamReader<ByteBuffer> eventStreamReader;

    private String stream;

    private boolean stopped;
    private ReaderGroup readerGroup;

    private Position lastPosition;
    private ScheduledExecutorService backgroundExecutor;
    private CompletableFuture<Checkpoint> checkpoint;
    private int numRetries = 10;
    private int retryMillis = 1000;
    private long segment = 0;
    private long position = 0;
    private long sequence = 0;
    private boolean restartable = false;
    private String key;
    private String secret;
    private String regionName;
    private S3Client s3;
    private String bucketName;
    private String scopeName;
    private String streamName;
    private String controllerUriText;
    private String username;
    private String password;
    private Integer numberOfSegments;

    public void start() {
        try {
            beforeRead();
            while (!stopped) {
                try {
             
                    EventRead<ByteBuffer> result = null;
                    doWithRetry(new Action<EventRead<ByteBuffer>>() {
                       @Override
                       public EventRead<ByteBuffer> execute() throws Exception {
                              return eventStreamReader.readNextEvent(1000);
                       }
                    }, numRetries, retryMillis, true);
                    lastPosition = result.getPosition();
                    if (result != null && result.isCheckpoint() == false && result.getEvent() != null) {
                        afterRead(result.getEvent());
                    }
                }
                catch (ReinitializationRequiredException e) {
                    // Expected
                }
                catch (IllegalStateException e) {
                    logger.error("Exception from retries: {}", e);
                    checkAndThrow(e);
                }
                catch (IllegalArgumentException e) {
                    logger.error("Invalid argument specified for reader {} : {}", readerId, e);
                    checkAndThrow(e);
                }
                catch (TruncatedDataException e) {
                    logger.error("Got a truncated data exception on forgetful reader {} after position {}", readerId, lastPosition, e);
                    checkAndThrow(e);
                }
                catch (SegmentTruncatedException e) {
                    logger.error("Segment is truncated: {}", e);
                    checkAndThrow(e);
                }
            }

            finished(null);
        }
        catch (Throwable e) {
            finished(e);
        }
    }

    private void checkAndThrow(Exception e) throws Exception {
         if (!restartable && !stopped) {
             throw e;
         }
    }

    private void prepare() {
        ReaderConfig readerConfig = ReaderConfig.builder()
            .build();

        readerId = UUID.randomUUID() + "-" + readerGroup.getScope() + "-" +  stream;
        eventStreamReader = clientFactory.createReader(readerId, readerGroup.getGroupName(), SERIALIZER, readerConfig);

        logger.info("Added Reader {} To ReaderGroup {}", readerId, readerGroup.getGroupName());
    }

    private void beforeRead() {
            Checkpoint cpResult = null;
            if (restartable) {
                try {
                   cpResult = checkpoint.get(5, TimeUnit.SECONDS);
                } catch(Exception e) {
                   logger.error("An exception occurred while getting next checkpoint: {}", e);
                }
            }
            // reset ReaderGroup
            ReaderGroupConfig.ReaderGroupConfigBuilder builder = ReaderGroupConfig.builder();
            StreamCut streamCut = getStreamCut(readerGroup.getScope(), stream, segment, position);
            builder = builder.stream(readerGroup.getScope() + "/" + stream, streamCut, StreamCut.UNBOUNDED);
            if (cpResult != null) {
                builder = builder.startFromCheckpoint(cpResult);
                logger.info("reader {} building from checkpoint", readerId);
            }
            ReaderGroupConfig resetConfig = builder.build();
            logger.info("Resetting ReaderGroup {} at position {}", readerGroup.getGroupName(), lastPosition);
            readerGroup.resetReaderGroup(resetConfig);
            prepare();
    }

    private StreamCut getStreamCut(String scope, String stream, long segmentId, long position) {
            logger.info("generating StreamCut for scope:{}, stream:{}, segment:{}, position:{}",
                      scope, stream, segmentId, position);
            Segment segment = new Segment(scope, stream, segmentId);
            Map<Segment, Long> segmentPositionMap = new HashMap<>();
            segmentPositionMap.put(segment, position);
            StreamCut streamCut = new StreamCutImpl(segment.getStream(), segmentPositionMap);
            return streamCut;
    }

    public void stop() {
        stopped = true;
    }

    private void afterRead(ByteBuffer payload) {
        logger.info("Reader read event of size: {}", payload.capacity());
        logger.debug(StandardCharsets.UTF_8.decode(payload).toString());
        try {
            sequence++;
            String objectKey = String.format("%030d", sequence);
            Region region = Region.US_EAST_1;
            putS3Object(s3, bucketName+"/"+readerId, objectKey, payload);
        } catch (Exception e) {
             logger.error("Exception:{}", e);
             throw e;
        }
    }
    public static  String putS3Object(S3Client s3, String bucketName, String objectKey, ByteBuffer event) {

        try {
            ByteArraySegment buf = new ByteArraySegment(ByteBufferUtils.slice(event, event.arrayOffset(), event.position()));
            PutObjectResponse response = s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build(),
                    RequestBody.fromByteBuffer(event));

            return response.eTag();

        } catch (S3Exception e) {
             logger.error("Exception: {}", e);
             throw e;
        }
    }

    private void finished(Throwable e) {
        if (e != null) {
            logger.error("Reader finished with Error", e);
        }
        else {
            logger.info("Reader finished");
        }

        try {
            readerGroup.readerOffline(readerId, lastPosition);
            logger.info("Removed Reader {} from ReaderGroup {} at position {}", readerId, readerGroup.getGroupName(), lastPosition);
        } catch (Throwable cleanupException) {
            logger.error("Couldn't cleanup finished reader {} at position {}", readerId, lastPosition, cleanupException);
        }
        finally {
            // raise a reader finished event
        }
    }

    public Reader withClientFactory(EventStreamClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        return this;
    }

    public Reader withReaderGroup(ReaderGroup readerGroup, int readerNum, int numRetries, int retryMillis, boolean restartable, long segment, long position) {
        this.readerGroup = readerGroup;
        this.numRetries = numRetries;
        this.retryMillis = (retryMillis < 1000) ? 1000 : retryMillis;
        this.segment = segment;
        this.position = position;
        this.restartable = restartable;
        if (restartable) {
            backgroundExecutor = newScheduledThreadPool(2, String.format("Checkpointer-%d",readerNum));
            checkpoint = readerGroup.initiateCheckpoint(String.format("Checkpoint-%d",readerNum), backgroundExecutor);
            logger.info("reader {} checkpoint initiated", String.format("Checkpoint-%d",readerNum));
        } else {
            logger.info( "reader {} with readerGroup not restartable", String.format("Checkpoint-%d",readerNum));
        }
        return this;
    }

    public Reader withStream(String stream) {
        this.stream = stream;
        return this;
    }
   
    public Reader withAWSCredentials(String key, String secret, String regionName, String bucketName) {
        this.key = key;
        this.secret = secret;
        this.regionName = regionName;
        this.bucketName = bucketName;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(key, secret);
        this.s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.US_EAST_1)
                .build();
        return this;
    }

    public Reader withPravegaProperties(String scopeName, String streamName, String controllerUriText, String username, String password, Integer numberOfSegments) {
        this.scopeName = scopeName;
        this.streamName = streamName;
        this.controllerUriText = controllerUriText;
        this.username = username;
        this.password = password;
        this.numberOfSegments = numberOfSegments;
        return this;
    }

    public ReaderGroup getReaderGroup() {
        return readerGroup;
    }

    public static  <T> T doWithRetry(Action<T> action, int retryCount, long sleepMillis, boolean printStackTrace) throws Exception {

        Throwable cause = null;

        for (int i = retryCount == 0 ? -1 : 0; i < retryCount; i++) {
            try {
                return action.execute();
            } catch (final Exception e) {
                cause = e;
                if (cause instanceof TruncatedDataException) {
                    throw e;
                }
                if (printStackTrace) {
                    logger.error("doWithRetry caught exception: {}", e);
                }
                else {
                    logger.error("doWithRetry caught exception with message {}", e.getMessage());
                }
                if (retryCount > 0 ) {
                    try {
                       Thread.sleep(sleepMillis);
                    } catch (InterruptedException ex) {
                      // Expected
                    }
                }
            }
            if (retryCount != 0 && i == retryCount-1) {
               logger.warn("doWithRetry exhausted");
            }
        }
        throw new IllegalStateException(cause);
    }

    @FunctionalInterface
    public interface Action<T>{
        T execute() throws Exception;
    }

    private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }

}

