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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
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

public class AuditApp {
    private static final Logger logger = LoggerFactory.getLogger(AuditApp.class);
    private static final int READER_TIMEOUT_MS = 2000;
    private static final int LISTENER_PORT = 63333;
    private static final int NUM_LINES = 20;

    private static void analyzeStream(List<String> entries) {
        SortedMap<String, Integer> histogram = new TreeMap<>();
        entries.stream().forEach(h -> { countAndPrint(histogram, h); });
    }

    private static void analyzeBatch(List<String> entries) {
        SortedMap<String, Integer> histogram = new TreeMap<>();
        List<String> selected = entries.stream()
                // .filter(x -> Integer.valueOf(getSubstring(x, "before")) > -24 )
                .collect(Collectors.toList());
        selected.stream().forEach(h -> {countAndPrint(histogram, h); });
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
        result += "\"before\":\"" + String.valueOf(hours.getHours()) +"\"";
        result += "}";
        return result;
    }

    private static String getSubstring(String message, String key) {
        int start = message.indexOf(key) + key.length() + 3;
        int end = message.indexOf("\"", start);
        if (start < 0 || end <= start || end == 0) System.err.println("ERROR");
        String result = message.substring(start, end);
        return result;
    }
    private static class Feeder implements Runnable {
        private Socket socket;

        Feeder(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket);
            try {
                Scanner in = new Scanner(socket.getInputStream());
                List<String> allMessages = new ArrayList<>();
                while (in.hasNextLine()) {
                    allMessages.add(in.nextLine());
                    if (allMessages.size() > NUM_LINES) {
                        write(Constants.DEFAULT_SCOPE, Constants.DEFAULT_STREAM_NAME, URI.create(Constants.DEFAULT_CONTROLLER_URI),
                                Constants.DEFAULT_ROUTING_KEY, allMessages);
                        allMessages = new ArrayList<>();
                    }
                }
                if (allMessages.size() > 0) {
                    write(Constants.DEFAULT_SCOPE, Constants.DEFAULT_STREAM_NAME, URI.create(Constants.DEFAULT_CONTROLLER_URI),
                            Constants.DEFAULT_ROUTING_KEY, allMessages);
                    allMessages = new ArrayList<>();
                }
            } catch (Exception e) {
                System.out.println("Error:" + socket);
            } finally {
                try { socket.close(); } catch (IOException e) {}
                System.out.println("Closed: " + socket);
            }
        }
    }
    public static void main(String argv[]) throws Exception {
        try (ServerSocket listener = new ServerSocket(LISTENER_PORT)) {
            System.out.println("The TCPConnector for Pravega is running...");
            ExecutorService pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new Feeder(listener.accept()));
            }
        }
    }
    public static void analyzeOffline() throws Exception {
        List<String> allTransformed = readAndTransform(Constants.DEFAULT_SCOPE, Constants.DEFAULT_STREAM_NAME, URI.create(Constants.DEFAULT_CONTROLLER_URI));
        analyzeStream(allTransformed);
        analyzeBatch(allTransformed);


    }

    public static void test() throws Exception {
        List<String> allMessages;
        try {
            allMessages = Files.readAllLines(Paths.get("auditdata.log"));
            List<String> allTransformed = allMessages
                    .stream()
                    .map(s -> transform(s))
                    .collect(Collectors.toList());
            System.err.println("ALL_TRANSFORMED_SIZE="+String.valueOf(allTransformed.size()));
            System.err.println("DATA="+allMessages.get(0));
            analyzeStream(allTransformed);
            analyzeBatch(allTransformed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(String scope, String streamName, URI controllerURI, String routingKey, List<String> messages) {
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
            messages.forEach(message -> {
                System.out.format("Writing message: '%s' with routing-key: '%s' to stream '%s / %s'%n",
                        message, routingKey, scope, streamName);
                final CompletableFuture writeFuture = writer.writeEvent(routingKey, message);
            });
        }
    }
    public static List<String> readAndTransform(String scope, String streamName, URI controllerURI) {
        List<String> allTransformedMessages = new ArrayList<>();
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
                        List<String> messages = new ArrayList<>();
                        messages.add(message);
                        write("srsScope", "srsStream", URI.create("tcp://10.247.142.138:9090"), "srsRoutingKey", messages);
                        allTransformedMessages.add(message);
                    }
                } catch (ReinitializationRequiredException e) {
                    //There are certain circumstances where the reader needs to be reinitialized
                    e.printStackTrace();
                }
            } while (event.getEvent() != null);
            System.out.format("No more events from %s/%s%n", scope, streamName);
        }
        return allTransformedMessages;
    }
}

/*
The TCPConnector for Pravega is running...
Connected: Socket[addr=/127.0.0.1,port=43004,localport=63333]
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ControllerImpl - Controller client connecting to server at 10.247.142.138:9090
[pool-1-thread-1] INFO io.pravega.client.admin.impl.StreamManagerImpl - Creating scope: auditScope
[StreamManager-Controller-1] INFO io.pravega.client.stream.impl.ControllerImpl - [requestId=3779208486958896153] Tagging client request (createScope-auditScope).
[StreamManager-Controller-1] INFO io.pravega.client.stream.impl.ControllerResolverFactory - Updating client with controllers: [[[/10.247.142.138:9090]/{}]]
[grpc-default-executor-0] WARN io.pravega.client.stream.impl.ControllerImpl - [requestId=3779208486958896153] Scope already exists: auditScope
[pool-1-thread-1] INFO io.pravega.client.admin.impl.StreamManagerImpl - Creating scope/stream: auditScope/auditStream with configuration: StreamConfiguration(scalingPolicy=ScalingPolicy(scaleType=FIXED_NUM_SEGMENTS, targetRate=0, scaleFactor=0, minNumSegments=1), retentionPolicy=null)
[StreamManager-Controller-1] INFO io.pravega.client.stream.impl.ControllerImpl - [requestId=1663452513411655884] Tagging client request (createStream-auditScope-auditStream).
[grpc-default-executor-0] WARN io.pravega.client.stream.impl.ControllerImpl - [requestId=1663452513411655884] Stream already exists: auditStream
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ControllerImpl - Controller client connecting to server at 10.247.142.138:9090
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ClientFactoryImpl - Creating writer for stream: auditStream with configuration: EventWriterConfig(initalBackoffMillis=1, maxBackoffMillis=20000, retryAttempts=10, backoffMultiple=10, enableConnectionPooling=true, transactionTimeoutTime=29999)
[pool-1-thread-1] INFO io.pravega.client.stream.impl.SegmentSelector - Refreshing segments for stream StreamImpl(scope=auditScope, streamName=auditStream)
[clientInternal-2-1] INFO io.pravega.client.stream.impl.ControllerResolverFactory - Updating client with controllers: [[[/10.247.142.138:9090]/{}]]
[clientInternal-2-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Fetching endpoint for segment auditScope/auditStream/0.#epoch.0, writer f142ab55-16af-4547-bf94-ac1bcc7c1743
Writing message: '{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"c6e7742b-dee4-4879-8d1f-b876fff54d6a","stage":"ResponseComplete","requestURI":"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc","verb":"get","user":{"username":"system:apiserver","uid":"cf4088dc-1ebe-4bef-a72a-448fb1e9d232","groups":["system:masters"]},"sourceIPs":["127.0.0.1"],"userAgent":"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946","objectRef":{"resource":"secrets","namespace":"cert-manager","name":"cert-manager-cainjector-token-t26jc","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2019-09-15T13:57:40.904647Z","stageTimestamp":"2019-09-15T13:57:40.905910Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}' with routing-key: 'auditRoutingKey' to stream 'auditScope / auditStream'
[clientInternal-2-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Establishing connection to PravegaNodeUri(endpoint=10.247.142.138, port=6000) for auditScope/auditStream/0.#epoch.0, writerID: f142ab55-16af-4547-bf94-ac1bcc7c1743
[clientInternal-2-1] INFO io.pravega.client.netty.impl.ConnectionPoolImpl - Creating a new connection to PravegaNodeUri(endpoint=10.247.142.138, port=6000)
[clientInternal-2-1] INFO io.pravega.client.netty.impl.FlowHandler - Creating Flow 1 for endpoint 10.247.142.138. The current Channel is null.
[epollEventLoopGroup-4-1] INFO io.pravega.client.netty.impl.FlowHandler - Connection established with endpoint 10.247.142.138 on ChannelId [id: 0x08a182f6]
[epollEventLoopGroup-4-1] INFO io.pravega.shared.protocol.netty.FailingReplyProcessor - Received hello: WireCommands.Hello(type=HELLO, highVersion=8, lowVersion=5)
[epollEventLoopGroup-4-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Received AppendSetup WireCommands.AppendSetup(type=APPEND_SETUP, requestId=4294967296, segment=auditScope/auditStream/0.#epoch.0, writerId=f142ab55-16af-4547-bf94-ac1bcc7c1743, lastEventNumber=-9223372036854775808)
[epollEventLoopGroup-4-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Connection setup complete for writer f142ab55-16af-4547-bf94-ac1bcc7c1743
Writing message: '{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"c6e7742b-dee4-4879-8d1f-b876fff54d6a","stage":"ResponseComplete","requestURI":"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc","verb":"get","user":{"username":"system:apiserver","uid":"cf4088dc-1ebe-4bef-a72a-448fb1e9d232","groups":["system:masters"]},"sourceIPs":["127.0.0.1"],"userAgent":"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946","objectRef":{"resource":"secrets","namespace":"cert-manager","name":"cert-manager-cainjector-token-t26jc","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2019-09-15T15:57:40.904647Z","stageTimestamp":"2019-09-15T15:57:40.905910Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}' with routing-key: 'auditRoutingKey' to stream 'auditScope / auditStream'
Writing message: '{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"c6e7742b-dee4-4879-8d1f-b876fff54d6a","stage":"ResponseComplete","requestURI":"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc","verb":"get","user":{"username":"system:apiserver","uid":"cf4088dc-1ebe-4bef-a72a-448fb1e9d232","groups":["system:masters"]},"sourceIPs":["127.0.0.1"],"userAgent":"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946","objectRef":{"resource":"secrets","namespace":"cert-manager","name":"cert-manager-cainjector-token-t26jc","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2019-09-15T13:57:40.904647Z","stageTimestamp":"2019-09-15T13:57:40.905910Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}' with routing-key: 'auditRoutingKey' to stream 'auditScope / auditStream'
[pool-1-thread-1] INFO io.pravega.client.stream.impl.Pinger - Closing Pinger periodic task
[pool-1-thread-1] INFO io.pravega.client.netty.impl.FlowHandler - Closing Flow 1 for endpoint 10.247.142.138
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ControllerResolverFactory - Shutting down ControllerNameResolver
[pool-1-thread-1] INFO io.pravega.client.netty.impl.ConnectionFactoryImpl - Shutting down connection factory
[pool-1-thread-1] INFO io.pravega.client.netty.impl.ConnectionPoolImpl - Shutting down connection pool
Closed: Socket[addr=/127.0.0.1,port=43004,localport=63333]The TCPConnector for Pravega is running...
Connected: Socket[addr=/127.0.0.1,port=43004,localport=63333]
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ControllerImpl - Controller client connecting to server at 10.247.142.138:9090
[pool-1-thread-1] INFO io.pravega.client.admin.impl.StreamManagerImpl - Creating scope: auditScope
[StreamManager-Controller-1] INFO io.pravega.client.stream.impl.ControllerImpl - [requestId=3779208486958896153] Tagging client request (createScope-auditScope).
[StreamManager-Controller-1] INFO io.pravega.client.stream.impl.ControllerResolverFactory - Updating client with controllers: [[[/10.247.142.138:9090]/{}]]
[grpc-default-executor-0] WARN io.pravega.client.stream.impl.ControllerImpl - [requestId=3779208486958896153] Scope already exists: auditScope
[pool-1-thread-1] INFO io.pravega.client.admin.impl.StreamManagerImpl - Creating scope/stream: auditScope/auditStream with configuration: StreamConfiguration(scalingPolicy=ScalingPolicy(scaleType=FIXED_NUM_SEGMENTS, targetRate=0, scaleFactor=0, minNumSegments=1), retentionPolicy=null)
[StreamManager-Controller-1] INFO io.pravega.client.stream.impl.ControllerImpl - [requestId=1663452513411655884] Tagging client request (createStream-auditScope-auditStream).
[grpc-default-executor-0] WARN io.pravega.client.stream.impl.ControllerImpl - [requestId=1663452513411655884] Stream already exists: auditStream
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ControllerImpl - Controller client connecting to server at 10.247.142.138:9090
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ClientFactoryImpl - Creating writer for stream: auditStream with configuration: EventWriterConfig(initalBackoffMillis=1, maxBackoffMillis=20000, retryAttempts=10, backoffMultiple=10, enableConnectionPooling=true, transactionTimeoutTime=29999)
[pool-1-thread-1] INFO io.pravega.client.stream.impl.SegmentSelector - Refreshing segments for stream StreamImpl(scope=auditScope, streamName=auditStream)
[clientInternal-2-1] INFO io.pravega.client.stream.impl.ControllerResolverFactory - Updating client with controllers: [[[/10.247.142.138:9090]/{}]]
[clientInternal-2-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Fetching endpoint for segment auditScope/auditStream/0.#epoch.0, writer f142ab55-16af-4547-bf94-ac1bcc7c1743
Writing message: '{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"c6e7742b-dee4-4879-8d1f-b876fff54d6a","stage":"ResponseComplete","requestURI":"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc","verb":"get","user":{"username":"system:apiserver","uid":"cf4088dc-1ebe-4bef-a72a-448fb1e9d232","groups":["system:masters"]},"sourceIPs":["127.0.0.1"],"userAgent":"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946","objectRef":{"resource":"secrets","namespace":"cert-manager","name":"cert-manager-cainjector-token-t26jc","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2019-09-15T13:57:40.904647Z","stageTimestamp":"2019-09-15T13:57:40.905910Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}' with routing-key: 'auditRoutingKey' to stream 'auditScope / auditStream'
[clientInternal-2-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Establishing connection to PravegaNodeUri(endpoint=10.247.142.138, port=6000) for auditScope/auditStream/0.#epoch.0, writerID: f142ab55-16af-4547-bf94-ac1bcc7c1743
[clientInternal-2-1] INFO io.pravega.client.netty.impl.ConnectionPoolImpl - Creating a new connection to PravegaNodeUri(endpoint=10.247.142.138, port=6000)
[clientInternal-2-1] INFO io.pravega.client.netty.impl.FlowHandler - Creating Flow 1 for endpoint 10.247.142.138. The current Channel is null.
[epollEventLoopGroup-4-1] INFO io.pravega.client.netty.impl.FlowHandler - Connection established with endpoint 10.247.142.138 on ChannelId [id: 0x08a182f6]
[epollEventLoopGroup-4-1] INFO io.pravega.shared.protocol.netty.FailingReplyProcessor - Received hello: WireCommands.Hello(type=HELLO, highVersion=8, lowVersion=5)
[epollEventLoopGroup-4-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Received AppendSetup WireCommands.AppendSetup(type=APPEND_SETUP, requestId=4294967296, segment=auditScope/auditStream/0.#epoch.0, writerId=f142ab55-16af-4547-bf94-ac1bcc7c1743, lastEventNumber=-9223372036854775808)
[epollEventLoopGroup-4-1] INFO io.pravega.client.segment.impl.SegmentOutputStreamImpl - Connection setup complete for writer f142ab55-16af-4547-bf94-ac1bcc7c1743
Writing message: '{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"c6e7742b-dee4-4879-8d1f-b876fff54d6a","stage":"ResponseComplete","requestURI":"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc","verb":"get","user":{"username":"system:apiserver","uid":"cf4088dc-1ebe-4bef-a72a-448fb1e9d232","groups":["system:masters"]},"sourceIPs":["127.0.0.1"],"userAgent":"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946","objectRef":{"resource":"secrets","namespace":"cert-manager","name":"cert-manager-cainjector-token-t26jc","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2019-09-15T15:57:40.904647Z","stageTimestamp":"2019-09-15T15:57:40.905910Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}' with routing-key: 'auditRoutingKey' to stream 'auditScope / auditStream'
Writing message: '{"kind":"Event","apiVersion":"audit.k8s.io/v1","level":"Metadata","auditID":"c6e7742b-dee4-4879-8d1f-b876fff54d6a","stage":"ResponseComplete","requestURI":"/api/v1/namespaces/cert-manager/secrets/cert-manager-cainjector-token-t26jc","verb":"get","user":{"username":"system:apiserver","uid":"cf4088dc-1ebe-4bef-a72a-448fb1e9d232","groups":["system:masters"]},"sourceIPs":["127.0.0.1"],"userAgent":"kube-apiserver/v1.13.5 (linux/amd64) kubernetes/2166946","objectRef":{"resource":"secrets","namespace":"cert-manager","name":"cert-manager-cainjector-token-t26jc","apiVersion":"v1"},"responseStatus":{"metadata":{},"code":200},"requestReceivedTimestamp":"2019-09-15T13:57:40.904647Z","stageTimestamp":"2019-09-15T13:57:40.905910Z","annotations":{"authorization.k8s.io/decision":"allow","authorization.k8s.io/reason":""}}' with routing-key: 'auditRoutingKey' to stream 'auditScope / auditStream'
[pool-1-thread-1] INFO io.pravega.client.stream.impl.Pinger - Closing Pinger periodic task
[pool-1-thread-1] INFO io.pravega.client.netty.impl.FlowHandler - Closing Flow 1 for endpoint 10.247.142.138
[pool-1-thread-1] INFO io.pravega.client.stream.impl.ControllerResolverFactory - Shutting down ControllerNameResolver
[pool-1-thread-1] INFO io.pravega.client.netty.impl.ConnectionFactoryImpl - Shutting down connection factory
[pool-1-thread-1] INFO io.pravega.client.netty.impl.ConnectionPoolImpl - Shutting down connection pool
Closed: Socket[addr=/127.0.0.1,port=43004,localport=63333]
*/



