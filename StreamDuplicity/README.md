# StreamDuplicity
This is an application to copy data from a stream store to S3.

## Overview

StreamDuplicity gives you tools to back up and restore your Pravega Streams to external store. You can run StreamDuplicity on a Kubernetes cluster or in standalone mode. It lets you:

* Take backups of your stream and restore in case of loss.
* Clone or migrate your stream between instances.
* Replicate your production data to development and testing clusters.

StreamDuplicity consists of:

* A Kubernetes chart to deploy the tool over any K8s cluster with or without other products
* A reader to read the stream for exporting the data

## Documentation

[The documentation]provides a getting started guide and information about building from source, architecture, extending StreamDuplicity, and more.

Please use the version selector at the top of the site to ensure you are using the appropriate documentation for your version of StreamDuplicity.

## Getting Started

This repository will have a chart and code for the data export tool.
The chart can be installed on the Kubernetes cluster with 
helm install --name streamduplicity ./charts -f ./values.yaml. 
If you are using SDP, you could prefer to use the nautilus-pravega namespace to load this chart so the controller api service is used within the same namespace. This tool does not make use of the Pravega cluster or SDP and merely accesses the controller api service in a read-only manner. 

The code for the data export tool can be run either in a standalone mode or when deployed to the Kubernetes cluster.
1) Standalone mode: 
   The standalone mode of the data export tool can be run just like any other Java application.
   Remember to build clean the jar with 
   `./gradlew --no-daemon clean jar`
   and run the jar using
   `java -jar build/libs/StreamDuplicity-1.0.jar`
   
   Please be sure to update the src/main/resources/config.properties file with all the parameters necessary to run this application.
   
2) Kubernetes cluster: 
   In the Kubernetes cluster deployment mode, the data export stream readers are already running. Their configuration to point to a stream to read can be applied via the same configuration file that is used for standalone mode or via environment variables. Both forms are supported and the deployment will provide a capability to allow you to upload a configuration file with a curl request to its api server and the readers will start running on the successful accept of the configuration and its load.

## StreamDuplicity API

The **StreamDuplicity** server listens on port `2020` to requests made to `/v1/duplicity/`, for the following resources:

| Method | Resource        | Request | Response |
|--------|-----------------|---------|----------|
| PUT    | /config         | InputStream |  **200** Accepted  |
| GET    | /restart/reader | N/A     | **200** Accepted + Boolean |
| POST   | /restart/reader | N/A     | **200** Accepted |
| GET    | /restart/writer | N/A     | **200** Accepted + Boolean |
| POST   | /restart/reader  | N/A     | **200** Accepted |

If you would like to run the standalone program without hosting on this on Kubernetes, you could set the following environment variables:
|    EnvVar    |    config.properties    |        Description              |
|--------------|-------------------------|---------------------------------|
| BUCKET_NAME  |    bucket_name          |  Specify just the name of the bucket, no ARN, or FQN hostname |
| AWS_ACCESS_KEY_ID | aws_access_key_id  |  Specify the access_key_id of the AWS client   |
| AWS__SECRET_ACCESS_KEY | aws__secret_access_key | Specify the secret access key of the client | 
| AWS_REGION | aws_region                | Specify the region of the S3 bucket such as 'us-region-1' |
| SCOPE_NAME | scope_name                | Specify the stream store scope name |
| STREAM_NAME | stream_name              | Specify the stream store stream name |
| CONTROLLER_URI | controller_uri        | Specify the controller uri in the form of protocol://<hostname/ip>:9090 |
| USERNAME       | pravega_username      | Specify the stream store admin username |
| PASSWORD       | pravega_password      | Specify the stream store admin password |
| NUMBER_OF_SEGMENTS | number_of_segments | Specify the number of segments in numerals only |
| STANDALONE     | standalone            | Specify the boolean true or false for running this program in standalone mode | 

## Troubleshooting

If you encounter issues, review the [troubleshooting docs], [file an issue], or talk to us on the #StreamDuplicity [coming shortly]  on the DELL-Storage Slack server.

## Contributing

If you are ready to jump in and test, add code, or help with documentation, follow the instructions on our [Start contributing] documentation for guidance on how to setup Velero for development.

## Changelog
[1] initial commit
