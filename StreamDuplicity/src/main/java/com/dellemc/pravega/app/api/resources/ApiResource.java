package com.dellemc.pravega.app.api.resources;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/duplicity")
@Produces(MediaType.APPLICATION_JSON)
public class ApiResource {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResource.class);

    private AtomicBoolean restartReader = new AtomicBoolean(false);
    private AtomicBoolean restartWriter = new AtomicBoolean(false);

    @Path("/config")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadConfig(final InputStream inputStream) {
            String configPath = "/data/config/config.properties";
            LOG.debug("Saving configuration: {}", configPath);
            if (inputStream == null) {
                return Response.status(400).entity("Invalid configuration").build();
            }

            try {
                try (FileWriter writer = new FileWriter(configPath)) {
                     String configuration = IOUtils.toString(inputStream);
                     LOG.debug("Writing: {}", configuration);
                     writer.write(configuration); 
                     LOG.debug("Configuration written, restarting...");
                }
                postRestartWriter();
                postRestartReader();
            } catch (final IOException e) {
                LOG.error("Failed to save file {}", configPath);
                return Response.status(500).entity(e.getMessage()).build();
            }
            LOG.debug("Configuration saved");
            return Response.ok().build();
    }

    @Path("/restart/reader")
    @GET
    public Response getRestartReader() {
        return Response.ok(String.valueOf(restartReader.get())).build();
    }

    @Path("/restart/writer")
    @GET
    public Response getRestartWriter() {
        return Response.ok(String.valueOf(restartWriter.get())).build();
    }

   @Path("/restart/reader")
   @POST
   public Response postRestartReader() {
       synchronized (this) {
           restartReader.getAndSet(true);
           try {
             Thread.sleep(2000);
           } catch (InterruptedException e) {
             LOG.error("Not enough time was given for restart");
           }
           restartReader.getAndSet(false);
       }
       LOG.info("Restart of readers initiated.");
       return Response.ok().build();
   }

   @Path("/restart/writer")
   @POST
   public Response postRestartWriter() {
       synchronized (this) {
           restartWriter.getAndSet(true);
           try {
             Thread.sleep(2000);
           } catch (InterruptedException e) {
             LOG.error("Not enough time was given for restart");
           }
           restartWriter.getAndSet(false);
       }
       LOG.info("Restart of writers initiated.");
       return Response.ok().build();
   }
}
