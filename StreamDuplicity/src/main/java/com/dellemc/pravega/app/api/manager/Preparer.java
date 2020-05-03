package com.dellemc.pravega.app.api.manager;

import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Singleton
public class Preparer {
    private static final Logger LOG = LoggerFactory.getLogger(Preparer.class);

    private final URI controllerUri;

    @Inject
    public Preparer(@Named("controllerUri") URI controllerUri) {
        this.controllerUri = controllerUri;
    }

}
