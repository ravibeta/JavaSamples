LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
APP_ALLOW_MISSING_DEPS:= true
LOCAL_MODULE := libtimechart
LOCAL_MODULE_TAGS := optional

LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_CPP_EXTENSION := .cc .cpp

LOCAL_SRC_FILES := \
                   core.cc \
                   src/core/base.cc \
                   src/core/command.cc \
                   src/core/connection.cc \
                   src/ipc/ipcserver.cc 

LOCAL_C_INCLUDES := \
                   $(LOCAL_PATH)/src \
                   $(LOCAL_PATH)/include \
                   $(LOCAL_PATH)/include/core \
                   $(LOCAL_PATH)/include/ipc \
                   $(JNI_H_INCLUDE)                   
                   
LOCAL_LDLIBS := -lz -llog

# stlport conflicts with the host stl library
ifneq ($(TARGET_SIMULATOR),true)
LOCAL_C_INCLUDES += external/stlport/stlport
LOCAL_SHARED_LIBRARIES += libstlport
endif

LOCAL_CFLAGS := -D_GLIBCXX_PERMIT_BACKWARD_HASH

# compile executeable binary for test 
include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_MODULE := libtimechart_pie
LOCAL_MODULE_TAGS := optional

LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_CPP_EXTENSION := .cc .cpp


LOCAL_SRC_FILES := \
                   core.cc \
                   src/core/base.cc \
                   src/core/connection.cc \
                   src/ipc/ipcserver.cc 

LOCAL_C_INCLUDES := \
                   $(LOCAL_PATH)/src \
                   $(LOCAL_PATH)/include \
                   $(LOCAL_PATH)/include/core \
                   $(LOCAL_PATH)/include/ipc \
                   $(JNI_H_INCLUDE)                   
                   
LOCAL_LDLIBS := -lz -llog

# stlport conflicts with the host stl library
ifneq ($(TARGET_SIMULATOR),true)
LOCAL_C_INCLUDES += external/stlport/stlport
LOCAL_SHARED_LIBRARIES += libstlport
endif

LOCAL_CFLAGS := -D_GLIBCXX_PERMIT_BACKWARD_HASH -DSUPPORT_ANDROID_21

# compatiable with L
LOCAL_LDFLAGS += -fPIC -pie

# compile executeable binary for test 
include $(BUILD_EXECUTABLE)

NDK_APPLICATION_MK=$(LOCAL_PATH)
