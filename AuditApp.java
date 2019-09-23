package com.dellemc.pravega.app;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import io.pravega.client.ClientConfig;
import io.pravega.client.ClientFactory;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventRead;
import io.pravega.client.stream.EventStreamReader;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ReaderConfig;
import io.pravega.client.stream.ReaderGroupConfig;
import io.pravega.client.stream.ReinitializationRequiredException;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.impl.JavaSerializer;
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.*;
import org.joda.time.*;
import org.joda.time.Hours;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditApp{
    private static final Logger logger = LoggerFactory.getLogger(AuditApp.class);
    private static final int READER_TIMEOUT_MS = 2000;
    protected static final String DEFAULT_SCOPE = "auditScope";
    protected static final String DEFAULT_STREAM_NAME = "auditStream";
    protected static final String DEFAULT_CONTROLLER_URI = "tcp://127.0.0.1:9090";    
    protected static final String DEFAULT_ROUTING_KEY = "auditRoutingKey";
    protected static final String DEFAULT_MESSAGE = "audit msg";

    private static void analyzeStream(List<String> entries) {
        SortedMap<String, Integer> histogram = new TreeMap<>();
        entries.stream().map(h -> { countAndPrint(histogram, h); return "success"; });
    }
    private static void analyzeBatch(List<String> entries) {
        SortedMap<String, Integer> histogram = new TreeMap<>();
        List<String> selected = entries.stream()
               // .filter(x -> Integer.valueOf(getSubstring(x, "before")) > -24 )
               .collect(Collectors.toList());
        selected.stream().map(h -> {countAndPrint(histogram, h); return "success"; });
    }
    public static void countAndPrint(SortedMap<String, Integer> histogram, String entry) {
           String hours = getSubstring(entry, "before");
           if (histogram.containsKey(hours)) {
               int count = histogram.get(hours);
               histogram.remove(hours);
               histogram.put(hours, count+1);
           } else {
               histogram.put(hours, 1);
           }
           PrintHistogram(histogram);
    }
    public static void PrintHistogram(SortedMap<String, Integer> histogram) {
           System.out.println();
           for (SortedMap.Entry<String, Integer> entry : histogram.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
           }
           System.out.println();
    }
   private static String transform(String message) {
        String result = "{";
        result += "\"username\": \"" + getSubstring(message, "username") + "\",";
        String timestamp = getSubstring(message, "requestReceivedTimestamp");
        DateTime dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(timestamp);
        DateTime current = DateTime.now(DateTimeZone.UTC);
        DateTimeFormatter patternFormat = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        .appendTimeZoneOffset("Z", true, 2, 4)
        .toFormatter();
        DateTime stamp = patternFormat.parseDateTime(timestamp);
        Hours hours = Hours.hoursBetween(current, stamp);
        result += "\"before\": \"" + String.valueOf(hours.getHours()) +"\"";
        result += "}";
        return result;
   }

   private static String getSubstring(String message, String key) {
        int start = message.indexOf(key) + key.length() + 3;
        int end = message.indexOf("\"", start);
        if (start < 0 || end < start || end == 0) System.err.println("ERROR");
        String result = message.substring(start, end);
        return result;
   }
    public static void main(String argv[]) throws Exception {
        List<String> allMessages;
        try {
                        allMessages = Files.readAllLines(Paths.get("auditdata.log"));
                        List<String> allTransformed = allMessages
                                                        .stream()
                                                        .map( s -> transform(s))
                                                        .collect(Collectors.toList());
                        analyzeStream(allTransformed);
                        analyzeBatch(allTransformed);
        } catch (IOException e) {
                        e.printStackTrace();
        }
}

public void write(String scope, String streamName, URI controllerURI, String routingKey, String message) {
        StreamManager streamManager = StreamManager.create(controllerURI);
        final boolean scopeIsNew = streamManager.createScope(scope);

        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(1))
                .build();
        final boolean streamIsNew = streamManager.createStream(scope, streamName, streamConfig);

        try (ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
             EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName,
                                                                                 new JavaSerializer<String>(),
                                                                                 EventWriterConfig.builder().build())) {
            
            System.out.format("Writing message: '%s' with routing-key: '%s' to stream '%s / %s'%n",
                    message, routingKey, scope, streamName);
            final CompletableFuture writeFuture = writer.writeEvent(routingKey, message);
        }
}

public void readAndTransform(String scope, String streamName, URI controllerURI) {
        StreamManager streamManager = StreamManager.create(controllerURI);
        
        final boolean scopeIsNew = streamManager.createScope(scope);
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(1))
                .build();
        final boolean streamIsNew = streamManager.createStream(scope, streamName, streamConfig);

        final String readerGroup = UUID.randomUUID().toString().replace("-", "");
        final ReaderGroupConfig readerGroupConfig = ReaderGroupConfig.builder()
                .stream(Stream.of(scope, streamName))
                .build();
        try (ReaderGroupManager readerGroupManager = ReaderGroupManager.withScope(scope, controllerURI)) {
            readerGroupManager.createReaderGroup(readerGroup, readerGroupConfig);
        }

        try (ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
             EventStreamReader<String> reader = clientFactory.createReader("reader",
                                                                           readerGroup,
                                                                           new JavaSerializer<String>(),
                                                                           ReaderConfig.builder().build())) {
            System.out.format("Reading all the events from %s/%s%n", scope, streamName);
            EventRead<String> event = null;
            do {
                try {
                    event = reader.readNextEvent(READER_TIMEOUT_MS);
                    if (event.getEvent() != null) {
                        System.out.format("Read event '%s'%n", event.getEvent());
			String message = transform(event.getEvent().toString());
			write("srsScope", "srsStream", URI.create("tcp://127.0.0.1:9090"), "srsRoutingKey", message);
                    }
                } catch (ReinitializationRequiredException e) {
                    //There are certain circumstances where the reader needs to be reinitialized
                    e.printStackTrace();
                }
            } while (event.getEvent() != null);
            System.out.format("No more events from %s/%s%n", scope, streamName);
        }
    }
}

