
package com.dellemc.pravega.app;

// import com.amazonaws.AmazonServiceException;
// import com.amazonaws.regions.Regions;
// import com.amazonaw.services.s3.AmazonS3;
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
import io.pravega.client.stream.TruncatedDataException;
import io.pravega.client.stream.impl.JavaSerializer;
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
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.function.Function;


public class StreamDuplicity {
    private static final Logger logger = LoggerFactory.getLogger(StreamDuplicity.class);
    // private static final AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION)

    private final JavaSerializer<String> SERIALIZER = new JavaSerializer<String>();

    private String readerId;
    private EventStreamClientFactory clientFactory;
    private EventStreamReader<String> eventStreamReader;

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
    private boolean restartable = false;

    public static void main(String argv[]) throws Exception {
	final ParameterTool params = ParameterTool.fromArgs(argv);
	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
        String scope = Constants.DEFAULT_SCOPE;
        String streamName = Constants.DEFAULT_STREAM_NAME;
        PravegaConfig pravegaConfig = PravegaConfig.fromParams(ParameterTool.fromArgs(argv));
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(Constants.NO_OF_SEGMENTS))
                .build();

        Stream stream = pravegaConfig.resolve(streamName);

        logger.info("creating a reader to read from stream");
        FlinkPravegaReader<String> flinkPravegaReader = FlinkPravegaReader.<String>builder()
                .withPravegaConfig(pravegaConfig)
                .forStream(stream)
                .withDeserializationSchema(PravegaSerialization.deserializationFor(String.class))
                .build();

        DataStream<String> eventsRead = env
                    .addSource(flinkPravegaReader)
                    .name("eventsRead");
        IterativeStream<String> it = eventsRead.iterate();
        List<String> dataList = new ArrayList<>();
        DataStream<String> newEvents = it.map(t -> {dataList.add(t); return t;});
        logger.info("count of events = {}", dataList.size());
        it.closeWith(newEvents);

        // try { 
        //      s3.putObject(bucket_name, key_name, new File(file_path));
        // } catch (AmazonServiceException e) {
        //      logger.error("Exception:{}", e);
        // }
        env.execute("Stream Writer");
    }

    public void start() {
        try {
            while (!stopped) {
                try {
                    beforeRead();
                    EventRead<String> result = doWithRetry(new Action<EventRead<String>>() {
                       @Override
                       public EventRead<String> execute() throws Exception {
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

    public void afterRead(String payload) {
        // collect statistics
    }

    public void finished(Throwable e) {
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

}

