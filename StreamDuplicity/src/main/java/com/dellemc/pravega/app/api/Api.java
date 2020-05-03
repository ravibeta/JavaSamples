package com.dellemc.pravega.app.api;

import com.dellemc.pravega.app.api.resources.ApiResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Api extends Application<ApiConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);

    public static void main(String[] args) {
        try {
            new Api().run(args);
        }
        catch (Exception e) {
            System.err.println("Error starting StreamDuplicity Api");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    @Override
    public void initialize(Bootstrap<ApiConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(ApiConfiguration configuration, Environment environment) throws Exception {
        Injector injector = createInjector(configuration);

        environment.jersey().register(new JsonProcessingExceptionMapper(true));
        environment.jersey().register(injector.getInstance(ApiResource.class));

        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(injector.getInstance(ObjectMapper.class));
        environment.jersey().register(provider);
    }

    private Injector createInjector(ApiConfiguration configuration) {
        return Guice.createInjector(new ApiModule(configuration));
    }
}
