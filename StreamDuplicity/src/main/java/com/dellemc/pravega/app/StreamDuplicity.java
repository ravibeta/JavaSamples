
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


public class StreamDuplicity {
    private static final Logger logger = LoggerFactory.getLogger(StreamDuplicity.class);
    // private static final AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION)

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

}

