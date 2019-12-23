package com.dellemc.pravega.app;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.examples.java.wordcount.util.WordCountData;
import org.apache.flink.api.java.DataSet;
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
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import io.pravega.connectors.flink.serialization.PravegaSerialization;

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

    public static void main(String argv[]) throws Exception {
	final ParameterTool params = ParameterTool.fromArgs(argv);
	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
        env.enableCheckpointing(1000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        DataSet<String> text; 
	String scope = Constants.DEFAULT_SCOPE;
	String streamName = Constants.DEFAULT_STREAM_NAME;
	PravegaConfig pravegaConfig = PravegaConfig.fromParams(ParameterTool.fromArgs(argv));
        StreamConfiguration streamConfig = StreamConfiguration.builder()
            .scalingPolicy(ScalingPolicy.fixed(Constants.NO_OF_SEGMENTS))
            .build();
	logger.info("001- creating stream");
        Stream stream = pravegaConfig.resolve(streamName);
	logger.info("002- creating stream manager for stream:{}", stream);
        Stream pravegaStream = stream;
	logger.info("003- creating FlinkPravegaWriter");

        FlinkPravegaWriter.Builder<GeneratedEvent> builder = FlinkPravegaWriter.<GeneratedEvent>builder()
            .withPravegaConfig(pravegaConfig)
            .forStream(pravegaStream)
            .withEventRouter(new GeneratedEventRouter())
            .withSerializationSchema(PravegaSerialization.serializationFor(GeneratedEvent.class));

        builder.enableWatermark(true);
        builder.withWriterMode(PravegaWriterMode.EXACTLY_ONCE);

        FlinkPravegaWriter<GeneratedEvent> pravegaWriter = builder.build();

	logger.info("004- generating data");
	List<GeneratedEvent> events = new ArrayList<GeneratedEvent>();
	events.add(new GeneratedEvent("key", Constants.DEFAULT_MESSAGE.replace("Event","NonEvent"), System.currentTimeMillis()));
        DataStream<GeneratedEvent> generatedEventStream =  env.fromCollection(events)
            .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<GeneratedEvent>() {
                public long extractAscendingTimestamp(GeneratedEvent generatedEvent) {
                    return generatedEvent.getTimestamp();
                }
            })
            .uid("watermark-generator");

        generatedEventStream.addSink(pravegaWriter)
            .uid("pravega-writer");

	logger.info("005- writing data");
	logger.info("006- writing data DONE");

        FlinkPravegaReader<String> flinkPravegaReader = FlinkPravegaReader.<String>builder()
                .withPravegaConfig(pravegaConfig)
                .forStream(pravegaStream)
                .withDeserializationSchema(PravegaSerialization.deserializationFor(String.class))
                .build();
        logger.info("007 - pravegaStreamReader created");

        DataStreamSink<String> eventsRead = env
                    .addSource(flinkPravegaReader)
                    .name("eventsRead")
                    .filter(new AuditApp.EventFilter())
                    .printToErr();

	logger.info("008 - pravegaStreamReader");
	env.execute("Stream Reader");
	logger.info("009- reading data DONE");
    }

    // filter non-events such as metadata and statistics from events in the stream.
    private static class EventFilter implements FilterFunction<String> {
        @Override
        public boolean filter(String line) throws Exception {
               return !line.contains("NonEvent");   
        }
    }
}
