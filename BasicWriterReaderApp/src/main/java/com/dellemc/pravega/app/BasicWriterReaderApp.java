
package com.dellemc.pravega.app;

import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.connectors.flink.PravegaWriterMode;
import io.pravega.connectors.flink.PravegaEventRouter;
import io.pravega.connectors.flink.serialization.PravegaSerialization;
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


public class BasicWriterReaderApp {
    private static final Logger logger = LoggerFactory.getLogger(BasicWriterReaderApp.class);

    public static void main(String argv[]) throws Exception {
	final ParameterTool params = ParameterTool.fromArgs(argv);
	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
        String scope = Constants.DEFAULT_SCOPE;
        String streamName = Constants.DEFAULT_STREAM_NAME;
        PravegaConfig pravegaConfig = PravegaConfig.fromParams(ParameterTool.fromArgs(argv));
        pravegaConfig.withCredentials(new DefaultCredentials(Constants.DEFAULT_PASSWORD, Constants.DEFAULT_USERNAME));
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(Constants.NO_OF_SEGMENTS))
                .build();

        logger.info("001- creating stream");
        Stream stream = pravegaConfig.resolve(streamName);

        logger.info("002- adding data");
        List<String> snippets = new ArrayList<>();
        snippets.add("2019-12-23 19:40:23,909 ERROR Line1");
        snippets.add("\tat org.apache.flink.client.program.PackagedProgram.callMainMethod(PackagedProgram.java:546)");
        snippets.add("\tat org.apache.flink.client.program.PackagedProgram.invokeInteractiveModeForExecution(PackagedProgram.java:421)");
        snippets.add("2019-12-23 19:40:24,557 INFO  org.apache.flink.runtime.rest.RestClient                      - Shutting down rest endpoint.");
        DataStream<String> input = env.fromCollection(snippets);
        input.print();

        logger.info("003- iterate over data");
        IterativeStream<String> iteration = input.iterate();
        iteration.withFeedbackType(String.class);
        List<String> entries = new ArrayList<>();
        DataStream<String> mapped = iteration.map(t -> {entries.add(t); return t;});
        for (String entry: entries) {
             logger.info("entry={}", entry);
        }
        logger.info("Number_of_elements={}", String.valueOf(entries.size()));
        iteration.closeWith(mapped);

        logger.info("004 - creating a writer to write to stream");
        FlinkPravegaWriter.Builder<String> builder = FlinkPravegaWriter.<String>builder()
            .withPravegaConfig(pravegaConfig)
            .forStream(stream)
                .withEventRouter(new PravegaEventRouter<String >() {
                    @Override
                    public String getRoutingKey(String e) {
                        return e;
                    }
                })
            .withSerializationSchema(PravegaSerialization.serializationFor(String.class));
        builder.enableWatermark(true);
        builder.withWriterMode(PravegaWriterMode.EXACTLY_ONCE);
        FlinkPravegaWriter<String> flinkPravegaWriter = builder.build();
        input.addSink(flinkPravegaWriter);
      
        java.lang.Thread.sleep(5000);
        logger.info("005 - creating a reader to read from stream");
        FlinkPravegaReader<String> flinkPravegaReader = FlinkPravegaReader.<String>builder()
                .withPravegaConfig(pravegaConfig)
                .forStream(stream)
                .withDeserializationSchema(PravegaSerialization.deserializationFor(String.class))
                .build();

        logger.info("006 - reading events from stream");
        DataStream<String> eventsRead = env
                    .addSource(flinkPravegaReader)
                    .name("eventsRead");
        IterativeStream<String> it = eventsRead.iterate();
        List<String> dataList = new ArrayList<>();
        DataStream<String> newEvents = it.map(t -> {dataList.add(t); return t;});
        logger.info("count of events = {}", dataList.size());
        it.closeWith(newEvents);

        logger.info("007- done");
        env.execute("Stream Writer");
    }

}

