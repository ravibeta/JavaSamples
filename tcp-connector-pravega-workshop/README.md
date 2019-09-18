# TCP-connector
This is a connector to send data to a stream in Pravega storage.

It can be used with nc utility

cat logfile | nc tcpserver tcpport

The tcpserver is ideally localhost
The tcpport is 8448

The TCP server writes data to the same stream but this can be customized with the help of a config file and reader.
The configuration would include:
pravega_endpoint: 
stream_name: 
scope : global
read_timeout_ms : 60000
username:
password:

The location of the config file would be in the resources

