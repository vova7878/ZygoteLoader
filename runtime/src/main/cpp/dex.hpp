#pragma once

#include <jni.h>
#include <stdint.h> // NOLINT(*-deprecated-headers)

jclass dex_load_and_init(JNIEnv *env, const char *package_name,
                         const void *dex_block, uint32_t dex_length,
                         const void *props_block, uint32_t props_length);

void dex_call_pre_specialize(JNIEnv *env, jclass entrypoint);

void dex_call_post_specialize(JNIEnv *env, jclass entrypoint);
