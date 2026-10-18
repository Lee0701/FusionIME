LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    app.cpp

LOCAL_MODULE := libchewing_android_app_module
LOCAL_SHARED_LIBRARIES := libchewing
LOCAL_MODULE_TAGS := optional
LOCAL_LDFLAGS += -llog

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/libs/libchewing/Android.mk
