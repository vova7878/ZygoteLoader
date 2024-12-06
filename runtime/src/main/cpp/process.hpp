#pragma once

#include <jni.h>

#define PACKAGE_NAME_SYSTEM_SERVER "android"
#define ALL_PACKAGES_NAME ".all"

void process_get_package_name(JNIEnv *env, jstring process_name, char **package_name);