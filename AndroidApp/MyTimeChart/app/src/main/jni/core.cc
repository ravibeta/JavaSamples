/**
 * @file core.cc
 * @brief Core library for TimeChart
 * @date 2020/12/26
 * @version 0.1
 * @author rravishankar@gmail.com
 *
 *  Main core program
 */


// Linux
#include <sys/types.h>

// STL
#include <vector>

// Android
#include <android/log.h>
#define APPNAME "TimeChart"
#include <connection.h>
#include <ipcserver.h>
#include <command.h>
#include <base.h>
#define BufferSize 256

// using name space
using namespace com::rearview::timechart;

// global variables
static ipc::ipcserver server;
static ipc::ipcMessage* command;

// system object
static std::vector<core::base *> adapter;

// for cache mechanize
struct cachedData {
  int id;
  int time;
};

static std::vector<cachedData *> storage;

// buffer
bool endLoop = false;
static char* buffer = NULL;
static int bufferSize = 0;

bool prepareIPC()
{
  // initialize
  bool result = false;

  result = server.init();
  if (!result)
  {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"can't initialize socket!\n");
    return (false);
  }

  // prepare socket
  if(!server.bind())
  {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"can't bind socket!\n");
    return (false);
  }

  return (true);
}

bool prepareBuffer(int requireSize)
{
  // check size
  if(requireSize > bufferSize)
  {
    // reallocate buffer
    if(buffer != NULL)
      delete buffer;
    buffer = new char[requireSize];

    // check buffer
    if(buffer == NULL)
      bufferSize = 0;
    else
      bufferSize = requireSize;
  }

  // valid buffer
  if(bufferSize == 0)
    return (false);

  // clean up
  memset(buffer, 0, bufferSize);
  return (true);
}

bool checkToken()
{
  // check token
   if(server.isVerified())
     return (true);

   server.checkToken();
   return (false);
}

bool receiveCMD()
{
  if(!prepareBuffer(4096))
    return (false);

  // receive data
  int recvSize = 0;
  if(!server.receieve(buffer, bufferSize, recvSize))
  {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"can't receive data!\n");
    return (false);
  }

  // verify buffer
  flatbuffers::Verifier verifier((const uint8_t*) buffer, (size_t) recvSize);
  if(!ipc::VerifyipcMessageBuffer(verifier))
  {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"can't parse data!\n");
    return (false);
  }

  // build instance
  command = (ipc::ipcMessage *) ipc::GetipcMessage(buffer);

  return (true);
}

bool sendData(char *data, int dataSize)
{
  if(!server.send(data, dataSize))
  {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "can't send data!\n");
    return (false);
  }
  return (true);
}

void initAdapter()
{
  // initial the array
  for (int i = 0; i < ipc::ipcCategory_FINAL; i++)
  {
    adapter.push_back(NULL);
    storage.push_back(NULL);
  }
}

bool prepareAdapter(ipc::ipcCategory action)
{
  // check and clean up when processing _R
  if (adapter[action] != NULL)
  {
    switch (action)
    {
    default:
      return (true);
    }
  }

  // prepare
  switch(action)
  {
    case ipc::ipcCategory_CONNECTION:
      adapter[ipc::ipcCategory_CONNECTION] =(core::base *) new core::connection();
      break;

    default:
      return (false);
  }

  return (true);
}

void cleanUp()
{
  // close all clients
  server.clean();

  // remove and empty all data
  for (int index = 0; index < ipc::ipcCategory_FINAL + 1; index++)
  {
    if (adapter[index] != NULL)
      delete adapter[index];
  }
  return;
}

bool processActionMsg()
{
  // current result
  FlatBufferBuilder flatBuffer;
  std::vector<Offset<ipc::ipcData>> resultList;
  bool flag = true;

  // process ACTION message
  for (int index = 0; index < command->data()->size(); index++)
  {

    // get data
    const ipc::ipcData *data = command->data()->Get(index);

    // check cache status and use cached data
    if (storage[data->category()] != NULL)
    {
      if (storage[data->category()]->id != server.getClientId() &&
          storage[data->category()]->time > (time(NULL) - 3))
      {
        auto result = flatBuffer.CreateVector(adapter[data->category()]->getData(),
                                              adapter[data->category()]->getSize());
        resultList.push_back(ipc::CreateipcData(flatBuffer, data->category(), result));
        continue;
      }
    }

    // prepare
    if (!prepareAdapter(data->category()))
    {
      flag = false;
      return flag;
    }

    // refresh
    (adapter[data->category()])->refresh();

    // add a new data
    auto rawData = flatBuffer.CreateVector(adapter[data->category()]->getData(),
                                           adapter[data->category()]->getSize());
    resultList.push_back(ipc::CreateipcData(flatBuffer, data->category(), rawData));

    // set cached data as empty
    if (storage[data->category()] != NULL) {
      delete storage[data->category()];
      storage[data->category()] = NULL;
    }

    // save data into cache storage
    cachedData *newCache = new cachedData();
    newCache->id = server.getClientId();
    newCache->time = time(NULL);
    storage[data->category()] = newCache;
    newCache = NULL;
  }

  // send data
  auto mloc = ipc::CreateipcMessage(flatBuffer, ipc::ipcType_RESULT, flatBuffer.CreateVector(resultList));
  ipc::FinishipcMessageBuffer(flatBuffer, mloc);

  if (!sendData((char *) flatBuffer.GetBufferPointer(), (int) flatBuffer.GetSize()))
  {
    server.close();
    flag = false;
  }

  return (flag);
}

bool processCommandMsg()
{
  // process ACTION message
  for (int index = 0; index < command->data()->size(); index++)
  {
    const ipc::ipcData *data = command->data()->Get(index);

    // skip invalid data
    flatbuffers::Verifier verifier(data->payload()->Data(), data->payload()->size());
    if(!com::rearview::timechart::core::VerifycommandInfoBuffer(verifier))
      continue;

    // TODO: core::command cmd(data->category(), core::GetcommandInfo(data->payload()->Data()));
    // TODO: cmd.execute();
  }
  return (true);
}

bool processCommand()
{
  // check token
  if(checkToken() == false)
    return (true);

  // receive ipcMessage
  if(receiveCMD() == false)
    return (true);

  // process Message
  switch(command->type())
  {

  // process EXIT message
  case ipc::ipcType_EXIT:
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"force Exit\n");
    endLoop = true;
    return (false);

  // process ACTION message
  case ipc::ipcType_ACTION:
    return (processActionMsg());

  case ipc::ipcType_COMMAND:
    return (processCommandMsg());

      case ipc::ipcType_RESULT:
        return (false);
  }

  return (false);
}

int main(int argc, char* argv[])
{
  if (argc != 4)
    return (1);

  // extract and erase Token
  server.extractToken(argv[1]);

  // extract and erase Socket name
  server.extractSocketName(argv[2]);

  // extract uid
  server.extractUid(argv[3]);

  // prepare IPC
  if(!prepareIPC())
    return (2);

  initAdapter();

  // receive commands
  endLoop = false;
  while(!endLoop)
  {

    ipc::ipcserver::EVENT event = server.poll();
    switch(event)
    {
    // error
    case ipc::ipcserver::ERROR:
      endLoop = true;
      break;

    // wait clients
    case ipc::ipcserver::WAIT:
      // do nothing
      break;

    // accept connections
    case ipc::ipcserver::CONNECTION:
      server.accept();
      break;

    // process command
    case ipc::ipcserver::COMMAND:
      processCommand();
      break;
    }
  }

  // clean up
  cleanUp();

  return (0);
}


