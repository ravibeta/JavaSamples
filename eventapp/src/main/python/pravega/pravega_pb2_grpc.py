# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import grpc

import pravega_pb2 as pravega__pb2


class PravegaGatewayStub(object):
  # missing associated documentation comment in .proto file
  pass

  def __init__(self, channel):
    """Constructor.

    Args:
      channel: A grpc.Channel.
    """
    self.CreateScope = channel.unary_unary(
        '/PravegaGateway/CreateScope',
        request_serializer=pravega__pb2.CreateScopeRequest.SerializeToString,
        response_deserializer=pravega__pb2.CreateScopeResponse.FromString,
        )
    self.CreateStream = channel.unary_unary(
        '/PravegaGateway/CreateStream',
        request_serializer=pravega__pb2.CreateStreamRequest.SerializeToString,
        response_deserializer=pravega__pb2.CreateStreamResponse.FromString,
        )
    self.UpdateStream = channel.unary_unary(
        '/PravegaGateway/UpdateStream',
        request_serializer=pravega__pb2.UpdateStreamRequest.SerializeToString,
        response_deserializer=pravega__pb2.UpdateStreamResponse.FromString,
        )
    self.ReadEvents = channel.unary_stream(
        '/PravegaGateway/ReadEvents',
        request_serializer=pravega__pb2.ReadEventsRequest.SerializeToString,
        response_deserializer=pravega__pb2.ReadEventsResponse.FromString,
        )
    self.WriteEvents = channel.stream_unary(
        '/PravegaGateway/WriteEvents',
        request_serializer=pravega__pb2.WriteEventsRequest.SerializeToString,
        response_deserializer=pravega__pb2.WriteEventsResponse.FromString,
        )
    self.GetStreamInfo = channel.unary_unary(
        '/PravegaGateway/GetStreamInfo',
        request_serializer=pravega__pb2.GetStreamInfoRequest.SerializeToString,
        response_deserializer=pravega__pb2.GetStreamInfoResponse.FromString,
        )
    self.BatchReadEvents = channel.unary_stream(
        '/PravegaGateway/BatchReadEvents',
        request_serializer=pravega__pb2.BatchReadEventsRequest.SerializeToString,
        response_deserializer=pravega__pb2.BatchReadEventsResponse.FromString,
        )


class PravegaGatewayServicer(object):
  # missing associated documentation comment in .proto file
  pass

  def CreateScope(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def CreateStream(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def UpdateStream(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def ReadEvents(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def WriteEvents(self, request_iterator, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def GetStreamInfo(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def BatchReadEvents(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')


def add_PravegaGatewayServicer_to_server(servicer, server):
  rpc_method_handlers = {
      'CreateScope': grpc.unary_unary_rpc_method_handler(
          servicer.CreateScope,
          request_deserializer=pravega__pb2.CreateScopeRequest.FromString,
          response_serializer=pravega__pb2.CreateScopeResponse.SerializeToString,
      ),
      'CreateStream': grpc.unary_unary_rpc_method_handler(
          servicer.CreateStream,
          request_deserializer=pravega__pb2.CreateStreamRequest.FromString,
          response_serializer=pravega__pb2.CreateStreamResponse.SerializeToString,
      ),
      'UpdateStream': grpc.unary_unary_rpc_method_handler(
          servicer.UpdateStream,
          request_deserializer=pravega__pb2.UpdateStreamRequest.FromString,
          response_serializer=pravega__pb2.UpdateStreamResponse.SerializeToString,
      ),
      'ReadEvents': grpc.unary_stream_rpc_method_handler(
          servicer.ReadEvents,
          request_deserializer=pravega__pb2.ReadEventsRequest.FromString,
          response_serializer=pravega__pb2.ReadEventsResponse.SerializeToString,
      ),
      'WriteEvents': grpc.stream_unary_rpc_method_handler(
          servicer.WriteEvents,
          request_deserializer=pravega__pb2.WriteEventsRequest.FromString,
          response_serializer=pravega__pb2.WriteEventsResponse.SerializeToString,
      ),
      'GetStreamInfo': grpc.unary_unary_rpc_method_handler(
          servicer.GetStreamInfo,
          request_deserializer=pravega__pb2.GetStreamInfoRequest.FromString,
          response_serializer=pravega__pb2.GetStreamInfoResponse.SerializeToString,
      ),
      'BatchReadEvents': grpc.unary_stream_rpc_method_handler(
          servicer.BatchReadEvents,
          request_deserializer=pravega__pb2.BatchReadEventsRequest.FromString,
          response_serializer=pravega__pb2.BatchReadEventsResponse.SerializeToString,
      ),
  }
  generic_handler = grpc.method_handlers_generic_handler(
      'PravegaGateway', rpc_method_handlers)
  server.add_generic_rpc_handlers((generic_handler,))
