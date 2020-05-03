package com.dellemc.pravega.app.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class ApiManager {
    private static final Logger LOG = LoggerFactory.getLogger(ApiManager.class);

    private final URI controllerUri;

    @Inject
    public ApiManager(
                      @Named("controllerUri") URI controllerUri,
                      @Named("configPath") String configPath,
                      ObjectMapper mapper) {
        this.controllerUri = controllerUri;
        LOG.info("ConfigPath={}", configPath);
        try (FileInputStream in = new FileInputStream(configPath)) {
            if (LOG.isDebugEnabled()){
                Path path = Paths.get(configPath);
                List<String> lines = Files.readAllLines(path);
                for (String text: lines) {
                    LOG.debug("Configuration:{}", text);
                }
            } 
            mapper.readValue(in, ApiConfiguration.class);
        } catch (Exception e) {
            LOG.error("Could not load configuration from {} due to : {}", configPath, e);
        }
    }
}
