#include "main_zygisk.h"

#include "logger.h"
#include "properties.h"
#include "process.h"
#include "logger.h"
#include "dex.h"

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <malloc.h>
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

    classesDex = resource_map_fd(classesDexFD);
    close(classesDexFD);
}

void ZygoteLoaderModule::reset() {
    free(currentProcessName);
    currentProcessName = nullptr;

    if (moduleProp != nullptr) {
        resource_release(moduleProp);
    }
    if (classesDex != nullptr) {
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
    if (currentProcessName != nullptr && classesDex != nullptr && moduleProp != nullptr) {
        LOGD("Loading in %s", currentProcessName);

        dex_load_and_invoke(
                env, currentProcessName,
                classesDex->base, classesDex->length,
                moduleProp->base, moduleProp->length
        );

        reset();
    }
}

static void extractInitializeData(char *data, const char *key, const char *value) {
    if (strcmp(key, "dataDirectory") == 0) {
        strcpy(data, value);
    }
}

bool ZygoteLoaderModule::shouldEnableForPackage(const char *packageName) {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    char dataDirectory[PATH_MAX] = {0};
    properties_for_each(
            moduleProp->base, moduleProp->length, dataDirectory,
            reinterpret_cast<properties_for_each_block>(&extractInitializeData)
    );
    fatal_assert(strlen(dataDirectory) > 0);

    int dataDirectoryFD = open(dataDirectory, O_RDONLY | O_DIRECTORY);
    fatal_assert(dataDirectoryFD >= 0);

    char path[PATH_MAX] = {0};
    sprintf(path, "packages/%s", packageName);

    if (faccessat(moduleDirFD, path, F_OK, 0) != 0) {
        if (faccessat(dataDirectoryFD, path, F_OK, 0) != 0) {
            return false;
        }
    }

    close(dataDirectoryFD);

    return true;
}

void ZygoteLoaderModule::initialize() {
    int moduleDirFD = api->getModuleDir();
    fatal_assert(moduleDirFD >= 0);

    int modulePropFD = openat(moduleDirFD, "module.prop", O_RDONLY);
    fatal_assert(modulePropFD >= 0);

    moduleProp = resource_map_fd(modulePropFD);
    close(modulePropFD);
}


REGISTER_ZYGISK_MODULE(ZygoteLoaderModule)
