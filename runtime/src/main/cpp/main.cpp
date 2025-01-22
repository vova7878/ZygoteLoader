#include "main.hpp"

#include "logger.hpp"
#include "process.hpp"
#include "dex.hpp"

#include <fcntl.h>
#include <unistd.h>

#include <jni.h>
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

bool testPackage(int fd, const char *name) {
    char path[PATH_MAX] = {0};
    sprintf(path, "packages/%s", name);

    return faccessat(fd, path, F_OK, 0) == 0;
}

bool ZygoteLoaderModule::shouldEnable(const char *package_name) {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    bool enable = false;
    if (testPackage(moduleDirFD, package_name) ^
        testPackage(moduleDirFD, ALL_PACKAGES_NAME)) {
        enable = true;
    }

    close(moduleDirFD);

    return enable;
}

void ZygoteLoaderModule::tryLoadDex(const char *package_name) {
    if (!shouldEnable(package_name)) {
        return;
    }

    LOGD("Loading in %s", package_name);

    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    Resource dex(moduleDirFD, "classes.dex");
    Resource props(moduleDirFD, "module.prop");

    close(moduleDirFD);

    entrypoint = (jclass) env->NewGlobalRef(dex_load_and_init(
            env, package_name,
            dex.base, dex.length,
            props.base, props.length
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
