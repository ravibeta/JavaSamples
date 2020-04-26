package com.dellemc.pravega.app;
import java.net.URI;

public class Constants {
    // stream
    protected static final String DEFAULT_SCOPE = "project58";
    protected static final String DEFAULT_STREAM_NAME = "logstream2";
    protected static final URI CONTROLLER_URI = URI.create("tcp://pravega-controller.svc.local:9090");
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final int NO_OF_SEGMENTS = 1;
    // bucket
    protected static final String BUCKET_NAME = "bucketName";
    protected static final String KEY_NAME = "keyName";
    protected static final String FILE_PATH = "/path/file";
}

