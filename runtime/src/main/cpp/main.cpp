#include "main.hpp"

#include "logger.hpp"
#include "properties.hpp"
#include "process.hpp"
#include "logger.hpp"
#include "dex.hpp"

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h> // NOLINT(*-deprecated-headers)

void ZygoteLoaderModule::onLoad(zygisk::Api *_api, JNIEnv *_env) {
    api = _api;
    env = _env;

    initialize();
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

void ZygoteLoaderModule::fetchResources() {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    int classesDexFD = openat(moduleDirFD, "classes.dex", O_RDONLY);
    fatal_assert(classesDexFD >= 0);

    resource_map_fd(classesDex, classesDexFD);
    close(classesDexFD);
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

void ZygoteLoaderModule::prepareFork() {
    if (shouldEnableForPackage(currentProcessName)) {
        fetchResources();
    } else {
        reset();
    }

    LOGD("Request dlclose module");

    api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
}

void ZygoteLoaderModule::tryLoadDex() {
    if (currentProcessName != nullptr && classesDex.base != nullptr && moduleProp.base != nullptr) {
        LOGD("Loading in %s", currentProcessName);

        dex_load_and_invoke(
                env, currentProcessName,
                classesDex.base, classesDex.length,
                moduleProp.base, moduleProp.length
        );

        reset();
    }
}

static void extractInitializeData(char *data, const char *key, const char *value) {
    if (strcmp(key, "dataDirectory") == 0) {
        strcpy(data, value);
    }
}

bool ZygoteLoaderModule::shouldEnableForPackage(const char *packageName) const {
    // TODO: cache dataDirectory
    char dataDirectory[PATH_MAX] = {0};
    properties_for_each(
            moduleProp.base, moduleProp.length, dataDirectory,
            reinterpret_cast<properties_for_each_block>(&extractInitializeData)
    );
    fatal_assert(strlen(dataDirectory) > 0);

    char path[PATH_MAX] = {0};
    sprintf(path, "%s/packages/%s", dataDirectory, packageName);

    return access(path, F_OK) == 0;
}

void ZygoteLoaderModule::initialize() {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    int modulePropFD = openat(moduleDirFD, "module.prop", O_RDONLY);
    fatal_assert(modulePropFD >= 0);

    resource_map_fd(moduleProp, modulePropFD);
    close(modulePropFD);
}


REGISTER_ZYGISK_MODULE(ZygoteLoaderModule)
