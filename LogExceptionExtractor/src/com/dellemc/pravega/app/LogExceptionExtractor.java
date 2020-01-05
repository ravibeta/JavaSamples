
package com.dellemc.pravega.app;

import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.connectors.flink.serialization.PravegaSerialization;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.TimestampExtractor;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import io.pravega.connectors.flink.PravegaConfig;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LogExceptionExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LogExceptionExtractor.class);

    public static void main(String argv[]) throws Exception {
	final ParameterTool params = ParameterTool.fromArgs(argv);
	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);

	logger.info("001- creating stream");
    List<String> snippets = new ArrayList<>();
    snippets.add("2019-12-23 19:40:23,909 ERROR Line1");
    snippets.add("\tat org.apache.flink.client.program.PackagedProgram.callMainMethod(PackagedProgram.java:546)");
    snippets.add("\tat org.apache.flink.client.program.PackagedProgram.invokeInteractiveModeForExecution(PackagedProgram.java:421)");
    snippets.add("2019-12-23 19:40:24,557 INFO  org.apache.flink.runtime.rest.RestClient                      - Shutting down rest endpoint.");
    DataStream<String> input = env.fromCollection(snippets);
    input.print();
    input.flatMap(new ExceptionTimestampExtractor()).print();
    input.flatMap(new ExceptionTimestampExtractor())
            .keyBy(new KeySelector<Tuple2<String, String>, String>() {

                @Override
                public String getKey(Tuple2<String, String> value) throws Exception {
                    return value._1;
                }
            })
            .window(EventTimeSessionWindows.withGap(Time.milliseconds(1)))
            .allowedLateness(Time.seconds(4))
            .process(new ProcessWindowFunction<Tuple2<String, String>, LogException, String, TimeWindow>() {
                Map<String, LogException> exceptionMap = new HashMap<>();
                         @Override
                         public void process(String s, Context context, Iterable<Tuple2<String, String>> elements, Collector<LogException> out) throws Exception {
                             LogException exception = null;
                            for(Tuple2<String, String> item : elements) {
                                if (exceptionMap.containsKey(item._1)) {
                                    exception = exceptionMap.get(item._1);
                                    exception.getData().add(item._2);
                                } else {
                                    exception = new LogException();
                                    exception.setTimestamp(item._1);
                                    exception.getData().add(item._2);
                                    exceptionMap.put(item._1, exception);
                                    out.collect(exception);
                                }
                                logger.info("t={},s={},e={}", item._1, item._2, exception);
                            }
                         }
                     }
            ).printToErr();
        env.execute("Stream Writer");
    }
/*
3> LogException{id='7e18159a-d941-416d-8ab6-7d3172595900', timestamp='2019-12-23 19:40:23,909', data='2019-12-23 19:40:23,909 ERROR Line1
'}
[Window(EventTimeSessionWindows(1), EventTimeTrigger, ProcessWindowFunction$1) -> Sink: Print to Std. Err (3/4)] ERROR com.dellemc.pravega.app.LogExceptionExtractor - t=2019-12-23 19:40:23,909,s=2019-12-23 19:40:23,909 ERROR Line1,e=LogException{id='7e18159a-d941-416d-8ab6-7d3172595900', timestamp='2019-12-23 19:40:23,909', data='2019-12-23 19:40:23,909 ERROR Line1
'}
3> LogException{id='212b9b92-4c31-47f9-be40-344156c2e593', timestamp='2020-01-05 10:34:47,574-08:00', data='	at org.apache.flink.client.program.PackagedProgram.invokeInteractiveModeForExecution(PackagedProgram.java:421)
'}
[Window(EventTimeSessionWindows(1), EventTimeTrigger, ProcessWindowFunction$1) -> Sink: Print to Std. Err (3/4)] ERROR com.dellemc.pravega.app.LogExceptionExtractor - t=2020-01-05 10:34:47,574-08:00,s=	at org.apache.flink.client.program.PackagedProgram.invokeInteractiveModeForExecution(PackagedProgram.java:421),e=LogException{id='212b9b92-4c31-47f9-be40-344156c2e593', timestamp='2020-01-05 10:34:47,574-08:00', data='	at org.apache.flink.client.program.PackagedProgram.invokeInteractiveModeForExecution(PackagedProgram.java:421)
'}
3> LogException{id='7f45f815-95b6-48a2-b72b-f39e4f037eb3', timestamp='2019-12-23 19:40:24,557', data='2019-12-23 19:40:24,557 INFO  org.apache.flink.runtime.rest.RestClient                      - Shutting down rest endpoint.
'}
[Window(EventTimeSessionWindows(1), EventTimeTrigger, ProcessWindowFunction$1) -> Sink: Print to Std. Err (3/4)] ERROR com.dellemc.pravega.app.LogExceptionExtractor - t=2019-12-23 19:40:24,557,s=2019-12-23 19:40:24,557 INFO  org.apache.flink.runtime.rest.RestClient                      - Shutting down rest endpoint.,e=LogException{id='7f45f815-95b6-48a2-b72b-f39e4f037eb3', timestamp='2019-12-23 19:40:24,557', data='2019-12-23 19:40:24,557 INFO  org.apache.flink.runtime.rest.RestClient                      - Shutting down rest endpoint.
'}
 */
}
