#pragma once

#include "raii.hpp"

#include <jni.h>
#include <stdint.h> // NOLINT(*-deprecated-headers)

jclass dex_load_and_init(JNIEnv *env, int module_dir, const char *entrypoint_name,
                         const char *package_name, const char *process_name,
                         RAIILink<RAIIFile> *files, jsize dex_count);

void call_pre_specialize(JNIEnv *env, jclass entrypoint);

void call_post_specialize(JNIEnv *env, jclass entrypoint);

char *get_string_data(JNIEnv *env, jstring value);
