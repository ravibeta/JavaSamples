package com.dellemc.pravega.app;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import scala.Tuple2;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionTimestampExtractor implements FlatMapFunction<String, Tuple2<String, String>> {
    String previous = null;
    @Override
    public void flatMap(String s, Collector<Tuple2<String, String>> collector) throws Exception {
        Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
        Matcher m = p.matcher(s);
        String timestamp = previous;
        if (m.find()) {
            timestamp = m.group(1);
        } else {
            timestamp = previous;
        }
        collector.collect(new Tuple2<>(timestamp, s));
    }
}
