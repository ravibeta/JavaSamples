// automatically generated by the FlatBuffers compiler, do not modify

#ifndef FLATBUFFERS_GENERATED_IPCMESSAGE_COM_rearview_timechart_IPC_H_
#define FLATBUFFERS_GENERATED_IPCMESSAGE_COM_rearview_timechart_IPC_H_

#include "flatbuffers/flatbuffers.h"


namespace com {
namespace rearview {
namespace timechart {
namespace ipc {

struct ipcData;
struct ipcMessage;

enum ipcCategory {
  /// Non-Exist
  ipcCategory_NONEXIST = 0,
  /// Connection 
  ipcCategory_CONNECTION = 1,
  /// End
  ipcCategory_FINAL = 99
};

inline const char **EnumNamesipcCategory() {
  static const char *names[] = { "NONEXIST", "CONNECTION", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "FINAL", nullptr };
  return names;
}

inline const char *EnumNameipcCategory(ipcCategory e) { return EnumNamesipcCategory()[e]; }

enum ipcType {
  /// ACTION 
  ipcType_ACTION = 0,
  /// RESULT 
  ipcType_RESULT = 1,
  /// COMMAND 
  ipcType_COMMAND = 2,
  /// EXIT 
  ipcType_EXIT = 10
};

inline const char **EnumNamesipcType() {
  static const char *names[] = { "ACTION", "RESULT", "COMMAND", "", "", "", "", "", "", "", "EXIT", nullptr };
  return names;
}

inline const char *EnumNameipcType(ipcType e) { return EnumNamesipcType()[e]; }

struct ipcData : private flatbuffers::Table {
  ipcCategory category() const { return static_cast<ipcCategory>(GetField<int8_t>(4, 0)); }
  const flatbuffers::Vector<uint8_t> *payload() const { return GetPointer<const flatbuffers::Vector<uint8_t> *>(6); }
  bool Verify(flatbuffers::Verifier &verifier) const {
    return VerifyTableStart(verifier) &&
           VerifyField<int8_t>(verifier, 4 /* category */) &&
           VerifyField<flatbuffers::uoffset_t>(verifier, 6 /* payload */) &&
           verifier.Verify(payload()) &&
           verifier.EndTable();
  }
};

struct ipcDataBuilder {
  flatbuffers::FlatBufferBuilder &fbb_;
  flatbuffers::uoffset_t start_;
  void add_category(ipcCategory category) { fbb_.AddElement<int8_t>(4, static_cast<int8_t>(category), 0); }
  void add_payload(flatbuffers::Offset<flatbuffers::Vector<uint8_t>> payload) { fbb_.AddOffset(6, payload); }
  ipcDataBuilder(flatbuffers::FlatBufferBuilder &_fbb) : fbb_(_fbb) { start_ = fbb_.StartTable(); }
  ipcDataBuilder &operator=(const ipcDataBuilder &);
  flatbuffers::Offset<ipcData> Finish() {
    auto o = flatbuffers::Offset<ipcData>(fbb_.EndTable(start_, 2));
    return o;
  }
};

inline flatbuffers::Offset<ipcData> CreateipcData(flatbuffers::FlatBufferBuilder &_fbb,
   ipcCategory category = ipcCategory_NONEXIST,
   flatbuffers::Offset<flatbuffers::Vector<uint8_t>> payload = 0) {
  ipcDataBuilder builder_(_fbb);
  builder_.add_payload(payload);
  builder_.add_category(category);
  return builder_.Finish();
}

struct ipcMessage : private flatbuffers::Table {
  ipcType type() const { return static_cast<ipcType>(GetField<int8_t>(4, 0)); }
  const flatbuffers::Vector<flatbuffers::Offset<ipcData>> *data() const { return GetPointer<const flatbuffers::Vector<flatbuffers::Offset<ipcData>> *>(6); }
  bool Verify(flatbuffers::Verifier &verifier) const {
    return VerifyTableStart(verifier) &&
           VerifyField<int8_t>(verifier, 4 /* type */) &&
           VerifyField<flatbuffers::uoffset_t>(verifier, 6 /* data */) &&
           verifier.Verify(data()) &&
           verifier.VerifyVectorOfTables(data()) &&
           verifier.EndTable();
  }
};

struct ipcMessageBuilder {
  flatbuffers::FlatBufferBuilder &fbb_;
  flatbuffers::uoffset_t start_;
  void add_type(ipcType type) { fbb_.AddElement<int8_t>(4, static_cast<int8_t>(type), 0); }
  void add_data(flatbuffers::Offset<flatbuffers::Vector<flatbuffers::Offset<ipcData>>> data) { fbb_.AddOffset(6, data); }
  ipcMessageBuilder(flatbuffers::FlatBufferBuilder &_fbb) : fbb_(_fbb) { start_ = fbb_.StartTable(); }
  ipcMessageBuilder &operator=(const ipcMessageBuilder &);
  flatbuffers::Offset<ipcMessage> Finish() {
    auto o = flatbuffers::Offset<ipcMessage>(fbb_.EndTable(start_, 2));
    return o;
  }
};

inline flatbuffers::Offset<ipcMessage> CreateipcMessage(flatbuffers::FlatBufferBuilder &_fbb,
   ipcType type = ipcType_ACTION,
   flatbuffers::Offset<flatbuffers::Vector<flatbuffers::Offset<ipcData>>> data = 0) {
  ipcMessageBuilder builder_(_fbb);
  builder_.add_data(data);
  builder_.add_type(type);
  return builder_.Finish();
}

inline const ipcMessage *GetipcMessage(const void *buf) { return flatbuffers::GetRoot<ipcMessage>(buf); }

inline bool VerifyipcMessageBuffer(flatbuffers::Verifier &verifier) { return verifier.VerifyBuffer<ipcMessage>(); }

inline void FinishipcMessageBuffer(flatbuffers::FlatBufferBuilder &fbb, flatbuffers::Offset<ipcMessage> root) { fbb.Finish(root); }

}  // namespace ipc
}  // namespace timechart
}  // namespace rearview
}  // namespace com

#endif  // FLATBUFFERS_GENERATED_IPCMESSAGE_COM_rearview_timechart_IPC_H_