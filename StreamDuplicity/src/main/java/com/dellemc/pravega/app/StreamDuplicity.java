
package com.dellemc.pravega.app;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
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
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
    private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();
    private static EventStreamClientFactory clientFactory;
    private static ReaderGroup readerGroup;
    private static ReaderGroupManager readerGroupManager;
    private static int readerNum = 1;
    private static int numRetries = 10;
    private static int retryMillis = 100; 
    private static boolean restartable = true;
    private static long segment = 0L;
    private static long position = 0L;

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

        ClientConfig clientConfig = ClientConfig.builder()
            .credentials(adminCredentials())
            .controllerURI(Constants.CONTROLLER_URI).build();
        clientFactory = EventStreamClientFactory.withScope(scope, clientConfig);
        if (readerGroupManager == null) {
            readerGroupManager = ReaderGroupManager.withScope(scope, clientConfig);
        }
        String readerGroupName = "data-transfer-reader-group";
        makeReaderGroup(readerGroupName);
        Stream stream = pravegaConfig.resolve(streamName);
        final Reader reader = new Reader()
                .withClientFactory(clientFactory)
                .withReaderGroup(readerGroup, readerNum, numRetries, retryMillis, restartable, segment, position)
                .withStream(streamName);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
               reader.start();
        });
        logger.info("Started reader");
        Futures.await(future);

    }

    private static void makeReaderGroup(String readerGroupName) {
        if (readerGroup == null) {
            readerGroup = readerGroupManager.getReaderGroup(readerGroupName);
            logger.info("created readerGroup {}", readerGroup.getGroupName());
        } else {
            logger.info("using existing readerGroup {}", readerGroup.getGroupName());
        }
    }

    public static Credentials adminCredentials() {
        return new DefaultCredentials(Constants.PASSWORD, Constants.USERNAME);
    }

    private static StreamManager streamManager() {
        return StreamManager.create(ClientConfig.builder()
            .credentials(adminCredentials())
            .controllerURI(Constants.CONTROLLER_URI)
            .build());
    }
}
