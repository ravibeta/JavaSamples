// automatically generated by the FlatBuffers compiler, do not modify

#ifndef FLATBUFFERS_GENERATED_CONNECTIONINFO_COM_EOLWRAL_OSMONITOR_CORE_H_
#define FLATBUFFERS_GENERATED_CONNECTIONINFO_COM_EOLWRAL_OSMONITOR_CORE_H_

#include "flatbuffers/flatbuffers.h"


namespace com {
namespace eolwral {
namespace osmonitor {
namespace core {

struct connectionInfo;
struct connectionInfoList;

enum connectionType {
  /// TCP version 4
  connectionType_TCPv4 = 0,
  /// TCP version 6
  connectionType_TCPv6 = 1,
  /// UDP version 4
  connectionType_UDPv4 = 2,
  /// UDP version 6
  connectionType_UDPv6 = 3,
  /// RAW version 4
  connectionType_RAWv4 = 4,
  /// RAW version 6
  connectionType_RAWv6 = 5
};

inline const char **EnumNamesconnectionType() {
  static const char *names[] = { "TCPv4", "TCPv6", "UDPv4", "UDPv6", "RAWv4", "RAWv6", nullptr };
  return names;
}

inline const char *EnumNameconnectionType(connectionType e) { return EnumNamesconnectionType()[e]; }

enum connectionStatus {
  connectionStatus_UNKNOWN = 0,
  connectionStatus_ESTABLISHED = 1,
  connectionStatus_SYN_SENT = 2,
  connectionStatus_SYN_RECV = 3,
  connectionStatus_FIN_WAIT1 = 4,
  connectionStatus_FIN_WAIT2 = 5,
  connectionStatus_TIME_WAIT = 6,
  connectionStatus_CLOSE = 7,
  connectionStatus_CLOSE_WAIT = 8,
  connectionStatus_LAST_ACK = 9,
  connectionStatus_LISTEN = 10,
  connectionStatus_CLOSING = 11
};

inline const char **EnumNamesconnectionStatus() {
  static const char *names[] = { "UNKNOWN", "ESTABLISHED", "SYN_SENT", "SYN_RECV", "FIN_WAIT1", "FIN_WAIT2", "TIME_WAIT", "CLOSE", "CLOSE_WAIT", "LAST_ACK", "LISTEN", "CLOSING", nullptr };
  return names;
}

inline const char *EnumNameconnectionStatus(connectionStatus e) { return EnumNamesconnectionStatus()[e]; }

struct connectionInfo : private flatbuffers::Table {
  connectionType type() const { return static_cast<connectionType>(GetField<int8_t>(4, 0)); }
  connectionStatus status() const { return static_cast<connectionStatus>(GetField<int8_t>(6, 0)); }
  const flatbuffers::String *localIP() const { return GetPointer<const flatbuffers::String *>(8); }
  uint32_t localPort() const { return GetField<uint32_t>(10, 0); }
  const flatbuffers::String *remoteIP() const { return GetPointer<const flatbuffers::String *>(12); }
  uint32_t remotePort() const { return GetField<uint32_t>(14, 0); }
  uint32_t uid() const { return GetField<uint32_t>(16, 0); }
  bool Verify(flatbuffers::Verifier &verifier) const {
    return VerifyTableStart(verifier) &&
           VerifyField<int8_t>(verifier, 4 /* type */) &&
           VerifyField<int8_t>(verifier, 6 /* status */) &&
           VerifyField<flatbuffers::uoffset_t>(verifier, 8 /* localIP */) &&
           verifier.Verify(localIP()) &&
           VerifyField<uint32_t>(verifier, 10 /* localPort */) &&
           VerifyField<flatbuffers::uoffset_t>(verifier, 12 /* remoteIP */) &&
           verifier.Verify(remoteIP()) &&
           VerifyField<uint32_t>(verifier, 14 /* remotePort */) &&
           VerifyField<uint32_t>(verifier, 16 /* uid */) &&
           verifier.EndTable();
  }
};

struct connectionInfoBuilder {
  flatbuffers::FlatBufferBuilder &fbb_;
  flatbuffers::uoffset_t start_;
  void add_type(connectionType type) { fbb_.AddElement<int8_t>(4, static_cast<int8_t>(type), 0); }
  void add_status(connectionStatus status) { fbb_.AddElement<int8_t>(6, static_cast<int8_t>(status), 0); }
  void add_localIP(flatbuffers::Offset<flatbuffers::String> localIP) { fbb_.AddOffset(8, localIP); }
  void add_localPort(uint32_t localPort) { fbb_.AddElement<uint32_t>(10, localPort, 0); }
  void add_remoteIP(flatbuffers::Offset<flatbuffers::String> remoteIP) { fbb_.AddOffset(12, remoteIP); }
  void add_remotePort(uint32_t remotePort) { fbb_.AddElement<uint32_t>(14, remotePort, 0); }
  void add_uid(uint32_t uid) { fbb_.AddElement<uint32_t>(16, uid, 0); }
  connectionInfoBuilder(flatbuffers::FlatBufferBuilder &_fbb) : fbb_(_fbb) { start_ = fbb_.StartTable(); }
  connectionInfoBuilder &operator=(const connectionInfoBuilder &);
  flatbuffers::Offset<connectionInfo> Finish() {
    auto o = flatbuffers::Offset<connectionInfo>(fbb_.EndTable(start_, 7));
    return o;
  }
};

inline flatbuffers::Offset<connectionInfo> CreateconnectionInfo(flatbuffers::FlatBufferBuilder &_fbb,
   connectionType type = connectionType_TCPv4,
   connectionStatus status = connectionStatus_UNKNOWN,
   flatbuffers::Offset<flatbuffers::String> localIP = 0,
   uint32_t localPort = 0,
   flatbuffers::Offset<flatbuffers::String> remoteIP = 0,
   uint32_t remotePort = 0,
   uint32_t uid = 0) {
  connectionInfoBuilder builder_(_fbb);
  builder_.add_uid(uid);
  builder_.add_remotePort(remotePort);
  builder_.add_remoteIP(remoteIP);
  builder_.add_localPort(localPort);
  builder_.add_localIP(localIP);
  builder_.add_status(status);
  builder_.add_type(type);
  return builder_.Finish();
}

struct connectionInfoList : private flatbuffers::Table {
  const flatbuffers::Vector<flatbuffers::Offset<connectionInfo>> *list() const { return GetPointer<const flatbuffers::Vector<flatbuffers::Offset<connectionInfo>> *>(4); }
  bool Verify(flatbuffers::Verifier &verifier) const {
    return VerifyTableStart(verifier) &&
           VerifyField<flatbuffers::uoffset_t>(verifier, 4 /* list */) &&
           verifier.Verify(list()) &&
           verifier.VerifyVectorOfTables(list()) &&
           verifier.EndTable();
  }
};

struct connectionInfoListBuilder {
  flatbuffers::FlatBufferBuilder &fbb_;
  flatbuffers::uoffset_t start_;
  void add_list(flatbuffers::Offset<flatbuffers::Vector<flatbuffers::Offset<connectionInfo>>> list) { fbb_.AddOffset(4, list); }
  connectionInfoListBuilder(flatbuffers::FlatBufferBuilder &_fbb) : fbb_(_fbb) { start_ = fbb_.StartTable(); }
  connectionInfoListBuilder &operator=(const connectionInfoListBuilder &);
  flatbuffers::Offset<connectionInfoList> Finish() {
    auto o = flatbuffers::Offset<connectionInfoList>(fbb_.EndTable(start_, 1));
    return o;
  }
};

inline flatbuffers::Offset<connectionInfoList> CreateconnectionInfoList(flatbuffers::FlatBufferBuilder &_fbb,
   flatbuffers::Offset<flatbuffers::Vector<flatbuffers::Offset<connectionInfo>>> list = 0) {
  connectionInfoListBuilder builder_(_fbb);
  builder_.add_list(list);
  return builder_.Finish();
}

inline const connectionInfoList *GetconnectionInfoList(const void *buf) { return flatbuffers::GetRoot<connectionInfoList>(buf); }

inline bool VerifyconnectionInfoListBuffer(flatbuffers::Verifier &verifier) { return verifier.VerifyBuffer<connectionInfoList>(); }

inline void FinishconnectionInfoListBuffer(flatbuffers::FlatBufferBuilder &fbb, flatbuffers::Offset<connectionInfoList> root) { fbb.Finish(root); }

}  // namespace core
}  // namespace osmonitor
}  // namespace eolwral
}  // namespace com

#endif  // FLATBUFFERS_GENERATED_CONNECTIONINFO_COM_EOLWRAL_OSMONITOR_CORE_H_
