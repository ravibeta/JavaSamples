package com.dellemc.pravega.app;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionTimestampExtractor implements FlatMapFunction<String, Tuple2<String, String>> {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionTimestampExtractor.class);

    String previous = DateTime.now().toString().replaceAll("T", " ").replace(".",",");
    @Override
    public void flatMap(String s, Collector<Tuple2<String, String>> collector) throws Exception {
        //TODO: not assume ordered events
        Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\,\\d{3})");
        Matcher m = p.matcher(s);
        String timestamp = previous;
        if (m.find()) {
            timestamp = m.group(1);
        } else {
            timestamp = previous;
        }
        logger.info("timestamp={}, s={}", timestamp, s);
        collector.collect(new Tuple2<>(timestamp, s));
    }
}
