package com.dellemc.connector.tcp;
import io.pravega.client.ClientConfig;
import io.pravega.client.stream.impl.DefaultCredentials;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.ReaderGroupConfig;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.stream.Stream;
import java.net.*;
import java.io.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connector {
    private static final Logger logger = LoggerFactory.getLogger(Connector.class);

    private static void createReaderGroup() throws Exception {
      String pravega_endpoint = "https://localhost/pravega";
      String username = "admin";
      String password = "";
      String streamName = "connectorStream";
      String scope = "global";
      URI uri = java.net.URI.create(pravega_endpoint);
      ClientConfig clientConfig = ClientConfig.builder()
                                 .controllerURI(uri)
                                 .credentials(new DefaultCredentials(password, username))
                                 .validateHostName(false)
                                 .build();
      StreamManager streamManager = StreamManager.create(clientConfig);
      streamManager.createScope(scope);
      streamManager.createStream(scope, streamName, StreamConfiguration.builder().build());
      ReaderGroupConfig readGroupConfig = ReaderGroupConfig.builder().stream(Stream.of(scope, streamName)).build();
      ReaderGroupManager readerGroupManager = ReaderGroupManager.withScope(scope, uri);
      String groupName = UUID.randomUUID().toString().replace("-", "");
      readerGroupManager.createReaderGroup(groupName, readGroupConfig);
    }

    public static void main(String argv[]) throws Exception {
       String clientSentence;
       String capitalizedSentence;
       ServerSocket welcomeSocket = new ServerSocket(8448);

       while (true) {
          Socket connectionSocket = welcomeSocket.accept();
          BufferedReader inFromClient =
             new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
          clientSentence = inFromClient.readLine();
          System.out.println("Received: " + clientSentence);
          capitalizedSentence = clientSentence.toUpperCase() + 'n';
          outToClient.writeBytes(capitalizedSentence);
      }
   }
}
