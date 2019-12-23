
package com.dellemc.pravega.app;

import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.connectors.flink.PravegaWriterMode;
import io.pravega.connectors.flink.serialization.PravegaSerialization;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.collector.selector.OutputSelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
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
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;


public class LogExceptionExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LogExceptionExtractor.class);

    public static void main(String argv[]) throws Exception {
	final ParameterTool params = ParameterTool.fromArgs(argv);
	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
        env.enableCheckpointing(1000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        DataSet<String> text; 
	PravegaConfig pravegaConfig = PravegaConfig.fromParams(ParameterTool.fromArgs(argv));
        StreamConfiguration streamConfig = StreamConfiguration.builder()
            .scalingPolicy(ScalingPolicy.fixed(Constants.NO_OF_SEGMENTS))
            .build();

	 logger.info("001- creating stream");
     DataStream<LogException> exceptionDataStream = env.readTextFile("exceptions.log")
                .flatMap(new ExceptionTimestampExtractor())
                .keyBy(0)
                .window(EventTimeSessionWindows.withGap(Time.milliseconds(1)))
                .allowedLateness(Time.seconds(4))
                .process(new ProcessWindowFunction<Tuple2<String, String>, LogException, Tuple, TimeWindow>() {
                    @Override
                    public void process(Tuple tuple, Context context, Iterable<Tuple2<String, String>> iterable, Collector<LogException> collector) throws Exception {
                        if (getRuntimeContext() == null) {
                            setRuntimeContext((RuntimeContext) new LogException());
                        }
                        LogException logException = (LogException) getRuntimeContext();
                        for(Tuple2<String, String> item : iterable) {
                            logException.setTimestamp(item._1);
                            logException.getData().add(item._2);
                        }
                        collector.collect(logException);
                    }
                });

        FlinkPravegaWriter.Builder<LogException> builder = FlinkPravegaWriter.<LogException>builder()
            .withPravegaConfig(pravegaConfig)
            .forStream((Stream) exceptionDataStream)
            .withEventRouter(new LogExceptionRouter())
            .withSerializationSchema(PravegaSerialization.serializationFor(LogException.class));


        builder.enableWatermark(true);
        builder.withWriterMode(PravegaWriterMode.EXACTLY_ONCE);
        FlinkPravegaWriter<LogException> flinkPravegaWriter = builder.build();
        exceptionDataStream.addSink(flinkPravegaWriter);
        env.execute("Stream Writer");
    }

}
