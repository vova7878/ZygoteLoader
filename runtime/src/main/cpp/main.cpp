#include "main.hpp"

#include "logger.hpp"
#include "process.hpp"
#include "dex.hpp"
#include "raii.hpp"

#include <jni.h>

#include <unistd.h>
#include <fcntl.h>

#include <errno.h> // NOLINT(*-deprecated-headers)
#include <string.h> // NOLINT(*-deprecated-headers)

void ZygoteLoaderModule::onLoad(zygisk::Api *_api, JNIEnv *_env) {
    api = _api;
    env = _env;

    LOGD("Request dlclose module");
    api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
}

void ZygoteLoaderModule::preAppSpecialize(zygisk::AppSpecializeArgs *args) {
    char *package_name;
    process_get_package_name(env, args->nice_name, &package_name);

    tryLoadDex(package_name);
    callJavaPreSpecialize();

    free(package_name);
}

void ZygoteLoaderModule::postAppSpecialize(const zygisk::AppSpecializeArgs *args) {
    callJavaPostSpecialize();
}

void ZygoteLoaderModule::preServerSpecialize(zygisk::ServerSpecializeArgs *args) {
    tryLoadDex(PACKAGE_NAME_SYSTEM_SERVER);
    callJavaPreSpecialize();
}

void ZygoteLoaderModule::postServerSpecialize(const zygisk::ServerSpecializeArgs *args) {
    callJavaPostSpecialize();
}

bool testPackage(int packages_dir, const char *name) {
    int res = faccessat(packages_dir, name, F_OK, 0);
    if (res == 0) return true;
    LOGI("faccessat returned %i and errno %s for package %s", res, strerror(errno), name);
    return false;
}

bool shouldEnable(int module_dir, const char *package_name) {
    int fd = openat(module_dir, "packages", O_PATH | O_DIRECTORY);
    if (fd == -1) {
        LOGI("openat returned %i and errno %s for package %s", fd, strerror(errno), package_name);
    }
    RAIIFD packages_dir = fd;
    return testPackage(packages_dir, package_name) ^
           testPackage(packages_dir, ALL_PACKAGES_NAME);
}

void ZygoteLoaderModule::tryLoadDex(const char *package_name) {
    RAIIFD module_dir = api->getModuleDir();

    if (!shouldEnable(module_dir, package_name)) {
        return;
    }

    LOGD("Loading in %s", package_name);

    RAIIFile dex(module_dir, "classes.dex");
    RAIIFile props(module_dir, "module.prop");

    entrypoint = (jclass) env->NewGlobalRef(dex_load_and_init(
            env, package_name,
            dex.data, dex.length,
            props.data, props.length
    ));
}

void ZygoteLoaderModule::callJavaPreSpecialize() {
    if (entrypoint != nullptr) {
        dex_call_pre_specialize(env, entrypoint);
    }
}

void ZygoteLoaderModule::callJavaPostSpecialize() {
    if (entrypoint != nullptr) {
        dex_call_post_specialize(env, entrypoint);

        env->DeleteGlobalRef(entrypoint);
        entrypoint = nullptr;
    }
}

REGISTER_ZYGISK_MODULE(ZygoteLoaderModule)
