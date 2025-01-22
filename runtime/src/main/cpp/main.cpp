#include "main.hpp"

#include "logger.hpp"
#include "process.hpp"
#include "dex.hpp"

#include <jni.h>

#include <unistd.h>
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

bool testPackage(int module_dir, const char *name) {
    char path[PATH_MAX] = {0};
    sprintf(path, "packages/%s", name);

    return faccessat(module_dir, path, F_OK, 0) == 0;
}

bool shouldEnable(int module_dir, const char *package_name) {
    return testPackage(module_dir, package_name) ^
           testPackage(module_dir, ALL_PACKAGES_NAME);
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
