/**
 * @file command.cpp
 * @brief Command Class file
 */

// #include <processor.h>
#include <command.h>
#include <android/log.h>


namespace com {
namespace rearview {
namespace timechart {
namespace core {

  command::command(ipc::ipcCategory category, const commandInfo* info)
  {
    this->category = category;
    this->info = (commandInfo *) info;
  }

  void command::setPriority()
  {

    if (info->arguments() == 0 || info->arguments()->size() < 2)
      return;

    int pid = atoi((*info->arguments()->Get(0)).c_str());
    int priority = atoi((*info->arguments()->Get(1)).c_str());

    setpriority(PRIO_PROCESS, pid, priority);
    return;
  }

  void command::killProcess()
  {
    if (info->arguments() == 0 || info->arguments()->Length() < 1)
      return;

    int pid = atoi((*info->arguments()->Get(0)).c_str());
    kill(pid, SIGKILL);
    return;
  }

  void command::execute()
  {
    if (info == NULL) return;

//    switch (category)
//    {
//      case ipc::ipcCategory_SETPRIORITY:
//        this->setPriority();
//        break;
//      case ipc::ipcCategory_KILLPROCESS:
//        this->killProcess();
//        break;
//        default:
//          break;
//    }
    return;
  }
}
}
}
}
