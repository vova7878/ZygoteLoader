#pragma once

#include <android/log.h>
#include <stdlib.h> // NOLINT(*-deprecated-headers)

#define TAG "ZygoteLoader[Native]"

#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGF(...) do { __android_log_print(ANDROID_LOG_FATAL, TAG, __VA_ARGS__); abort(); } while (0)
#else
#define LOGD(...)
#define LOGF(...) abort()
#endif

#define fatal_assert(expr) if (!(expr)) LOGF("!(" #expr ")")
