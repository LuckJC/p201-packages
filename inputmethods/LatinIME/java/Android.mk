# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := LatinIME

LOCAL_CERTIFICATE := shared

# We want to package libjni_latinime.so into the apk.
# No need package the lib into the apk
#LOCAL_JNI_SHARED_LIBRARIES := libjni_latinime
# We want to install libjni_latinime.so to the system partition if LatinIME gets installed.
LOCAL_REQUIRED_MODULES := libjni_latinime

LOCAL_STATIC_JAVA_LIBRARIES := android-common inputmethod-common android-support-v4

# Do not compress dictionary files to mmap dict data runtime
LOCAL_AAPT_FLAGS := -0 .dict

# Include all the resources regardless of system supported locales
LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

# LCA project will slim the dict files
ifneq ($(MTK_LCA_ROM_OPTIMIZE),yes)
res_dirs := res_base res
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
endif

LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)
