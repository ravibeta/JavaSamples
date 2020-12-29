/**
 * @file command.h
 * @brief Command Class header file
 */

#ifndef COMMAND_H_
#define COMMAND_H_

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <sys/resource.h>

#include "base.h"
#include "ipcMessage_generated.h"
#include "commandInfo_generated.h"

#define BufferSize 256

namespace com {
namespace rearview {
namespace timechart {
namespace core {

  /**
   * this is helper class for executing command from UI
   */
  class command
  {
  private:
    ipc::ipcCategory category;        /**< internal category */
    core::commandInfo* info;          /**< internal commandInfo */


  public:
    /**
     * initialize command object
     */
    command(ipc::ipcCategory category, const commandInfo *info);

    /**
     * set process priority
     */
    void setPriority();

    /**
     * kill process by pid
     */
    void killProcess();

    /**
     * execute command
     */
    void execute();
  };

}
}
}
}

#endif /* COMMAND_H */
