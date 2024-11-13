#include "main.hpp"

#include "logger.hpp"
#include "process.hpp"
#include "dex.hpp"

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h> // NOLINT(*-deprecated-headers)

void ZygoteLoaderModule::onLoad(zygisk::Api *_api, JNIEnv *_env) {
    api = _api;
    env = _env;
}

void ZygoteLoaderModule::preAppSpecialize(zygisk::AppSpecializeArgs *args) {
    process_get_package_name(env, args->nice_name, &currentProcessName);

    prepareFork();
}

void ZygoteLoaderModule::postAppSpecialize(const zygisk::AppSpecializeArgs *args) {
    tryLoadDex();
}

void ZygoteLoaderModule::preServerSpecialize(zygisk::ServerSpecializeArgs *args) {
    currentProcessName = strdup(PACKAGE_NAME_SYSTEM_SERVER);

    prepareFork();
}

void ZygoteLoaderModule::postServerSpecialize(const zygisk::ServerSpecializeArgs *args) {
    tryLoadDex();
}

bool ZygoteLoaderModule::shouldEnable() {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    char path[PATH_MAX] = {0};
    sprintf(path, "packages/%s", currentProcessName);

    return faccessat(moduleDirFD, path, F_OK, 0) == 0;
    close(moduleDirFD);
}

void ZygoteLoaderModule::fetchResources() {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    int modulePropFD = openat(moduleDirFD, "module.prop", O_RDONLY);
    fatal_assert(modulePropFD >= 0);
    resource_map_fd(moduleProp, modulePropFD);
    close(modulePropFD);

    int classesDexFD = openat(moduleDirFD, "classes.dex", O_RDONLY);
    fatal_assert(classesDexFD >= 0);
    resource_map_fd(classesDex, classesDexFD);
    close(classesDexFD);

    close(moduleDirFD);
}

void ZygoteLoaderModule::prepareFork() {
    if (shouldEnable()) {
        fetchResources();
    }

    LOGD("Request dlclose module");

    api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
}

void ZygoteLoaderModule::reset() {
    free(currentProcessName);
    currentProcessName = nullptr;

    if (moduleProp.base != nullptr) {
        resource_release(moduleProp);
    }
    if (classesDex.base != nullptr) {
        resource_release(classesDex);
    }
}

void ZygoteLoaderModule::tryLoadDex() {
    if (currentProcessName != nullptr
        && classesDex.base != nullptr
        && moduleProp.base != nullptr) {
        LOGD("Loading in %s", currentProcessName);

        dex_load_and_invoke(
                env, currentProcessName,
                classesDex.base, classesDex.length,
                moduleProp.base, moduleProp.length
        );
    }

    reset();
}

REGISTER_ZYGISK_MODULE(ZygoteLoaderModule)
