LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libchewing
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libchewing.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/capi/include

include $(PREBUILT_SHARED_LIBRARY)
