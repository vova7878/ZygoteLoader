#include "process.hpp"
#include <string.h> // NOLINT(*-deprecated-headers)

#include <android/log.h>

void process_get_package_name(JNIEnv *env, jstring process_name, char **package_name) {
    const char *str = env->GetStringUTFChars(process_name, nullptr);

    __android_log_print(ANDROID_LOG_WARN, "ZYGISK", "%s", str);

    *package_name = strdup(str);

    char *split = strchr(*package_name, ':');
    if (split != nullptr) {
        *split = '\0';
    }

    env->ReleaseStringUTFChars(process_name, str);
}
