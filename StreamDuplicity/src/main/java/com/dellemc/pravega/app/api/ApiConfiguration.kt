package com.dellemc.pravega.app.api;

import io.dropwizard.Configuration

data class ApiConfiguration (
    var pravegaControllerUri :String = "",
    var configPath: String = ""
): Configuration()
