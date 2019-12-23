This is an optional configuration for Administrators.

Logging-Service

This service exposes an http input endpoint for a designated stream in the stream store of the cluster. Administrators can use the host and port for this log service as the log destination in a [PKS cluster sink](https://docs.pivotal.io/pks/1-4/create-sinks.html)

Steps for the administrator to take.
1. Create a PKS cluster sink for logs
2. Point the log destination as the 
   host: logging-service.nautilus-system.svc.cluster.local
   port: 8080
3. The entries flow to a stream in the Pravega store which comes in helpful for diagnostics as it is part of the store.
4. The logs are never lost.

