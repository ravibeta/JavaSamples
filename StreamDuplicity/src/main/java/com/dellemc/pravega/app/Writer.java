package com.dellemc.pravega.app;

import com.google.common.base.Strings;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.Checkpoint;
import io.pravega.client.stream.EventRead;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.Position;
import io.pravega.client.stream.TransactionalEventStreamWriter;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ReinitializationRequiredException;
import io.pravega.client.stream.StreamCut;
import io.pravega.client.segment.impl.Segment;
import io.pravega.client.stream.impl.StreamCutImpl;
import io.pravega.client.segment.impl.SegmentTruncatedException;
import io.pravega.client.stream.TruncatedDataException;
import io.pravega.client.stream.impl.ByteBufferSerializer;
import io.pravega.client.stream.impl.JavaSerializer;
import io.pravega.client.stream.Serializer;
import io.pravega.common.util.ByteArraySegment;
import io.pravega.common.util.ByteBufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import static io.pravega.common.concurrent.ExecutorServiceHelpers.newScheduledThreadPool;

/**
 * Base class for Pravega Writer Workers that performs the basic stuff
 */
public class Writer {
    private static final Logger logger = LoggerFactory.getLogger(Writer.class);


    private final ByteBufferSerializer SERIALIZER = new ByteBufferSerializer();

    private String writerId;
    private EventStreamClientFactory clientFactory;
    private EventStreamWriter<ByteBuffer> eventStreamWriter;
    private String stream;
    private boolean stopped;
    private Position lastPosition;
    private ScheduledExecutorService backgroundExecutor;
    private CompletableFuture<Checkpoint> checkpoint;
    private int numRetries = 10;
    private int retryMillis = 1000;
    private long segment = 0;
    private long position = 0;
    private long sequence = 0;
    private boolean restartable = false;
    private String key;
    private String secret;
    private String regionName;
    private S3Client s3;
    private String bucketName;
    private String scopeName;
    private String streamName;
    private String controllerUriText;
    private String username;
    private String prefix;
    private String password;
    private Integer numberOfSegments;


    private static final long ONE_SECOND = TimeUnit.SECONDS.toMillis(1);

    private Consumer<Throwable> writerFinishedHandler;
    private String taskId = UUID.randomUUID().toString();

    public void prepare() {
        writerId = scopeName + "-" +  streamName + "-" + taskId;
        EventWriterConfig eventWriterConfig = EventWriterConfig.builder()
                .automaticallyNoteTime(false)
                .transactionTimeoutTime(30_000)
                .build();
        eventStreamWriter = clientFactory.createEventWriter(writerId, streamName, SERIALIZER, eventWriterConfig);
        logger.info("Writer {} prepared", writerId);
    }

    public void start() {
        logger.info("starting writer");
        try {
            int numNullEvents = 0;
            while (!stopped) {
                    CompletableFuture<Void> ackFuture = null;
                    ByteBuffer payload = beforeWrite();
                    ackFuture = writeEvent(payload);
                    afterWrite(ackFuture);
            }
            eventStreamWriter.flush();
            finished(null);
        }
        catch (Throwable e) {
            finished(e);
        }
        logger.info("stopping writer");
    }

    public void finished(Throwable e) {
        if (e != null) {
            logger.error(String.format("Writer finished with Error at sequence %d :", sequence),  e);
        } else {
            logger.info("Writer finished");
        }
    }

    public CompletableFuture<Void> writeEvent(ByteBuffer payload) {
        CompletableFuture<Void> writeEvent = eventStreamWriter.writeEvent("", payload);
        return writeEvent.whenComplete((res, ex) -> {
            if (ex == null) {
                logger.info("Writer wrote event of size: {}", payload.capacity());
                logger.debug("Writer wrote event with payload:{}", StandardCharsets.UTF_8.decode(payload).toString());
            }
            else {
                throw new RuntimeException("Exception in writing event to stream:", ex);
            }
        });
    }


    private ByteBuffer beforeWrite()  throws Exception {
        ByteBuffer payload = ByteBuffer.allocate(0);
        try {
            String objectKey = String.format("%030d", sequence);
            if (Strings.isNullOrEmpty(prefix)){
                prefix = scopeName+"/" + streamName + "/" + writerId + "/";
                logger.debug("using prefix={}", prefix);
            }
            logger.info("get S3 object :{} from bucket: {}", prefix+objectKey, bucketName);
            payload = getS3Object(s3, bucketName, prefix + objectKey);
            sequence++;
            logger.info("Writer got event of size: {}", payload.capacity());
            logger.debug("Writer got event with payload:{}", StandardCharsets.UTF_8.decode(payload).toString());
        } catch (Exception e) {
             logger.error("Exception:", e);
             throw e;
        }
        return payload;
    }

    private void afterWrite(CompletableFuture<Void> writeEvent) throws InterruptedException, ExecutionException {
        try {
          writeEvent.get();
        } catch (InterruptedException | ExecutionException e) {
          logger.error("write event interrupted:", e);
          throw e;
        }
    }

    public static ByteBuffer getS3Object(S3Client s3, String bucketName, String objectKey)  throws Exception {
        try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();
        GetObjectResponse response = s3.getObject(getObjectRequest, ResponseTransformer.toOutputStream(baos));
            ByteBuffer result = ByteBuffer.wrap(baos.toByteArray(), 0, baos.size());
            logger.debug("Got S3 object of size:{}", result.capacity());
            return result;

        } catch (S3Exception e) {
             logger.error("Exception: ", e);
             throw e;
        }
    }

    public Writer withClientFactory(EventStreamClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        return this;
    }
   
    public Writer withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public Writer withWriterSequence(int writerNum, int numRetries, int retryMillis, boolean restartable, long segment, long position, long sequence) {
        this.numRetries = numRetries;
        this.retryMillis = (retryMillis < 1000) ? 1000 : retryMillis;
        this.segment = segment;
        this.position = position;
        this.sequence = sequence;
        this.restartable = restartable;
        if (restartable) {
            backgroundExecutor = newScheduledThreadPool(2, String.format("Checkpointer-%d",writerNum));
            logger.info("writer {} checkpoint initiated", String.format("Checkpoint-%d",writerNum));
        } else {
            logger.info( "writer {} not restartable", String.format("Checkpoint-%d",writerNum));
        }
        return this;
    }

    public Writer withStream(String stream) {
        this.streamName = stream.replace("-copy","") + "-copy";
        return this;
    }

    public Writer withAWSCredentials(String key, String secret, String regionName, String bucketName) {
        this.key = key;
        this.secret = secret;
        this.regionName = regionName;
        this.bucketName = bucketName;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(key, secret);
        this.s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(regionName))
                .build();
        return this;
    }

    public Writer withPravegaProperties(String scopeName, String streamName, String controllerUriText, String username, String password, Integer numberOfSegments) {
        this.scopeName = scopeName;
        this.streamName = streamName;
        this.controllerUriText = controllerUriText;
        this.username = username;
        this.password = password;
        this.numberOfSegments = numberOfSegments;
        return this;
    }
}
