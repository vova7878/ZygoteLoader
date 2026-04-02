#include "main.hpp"

#include "logger.hpp"
#include "constants.hpp"
#include "dex.hpp"
#include "raii.hpp"

#include <jni.h>

#include <string.h> // NOLINT(*-deprecated-headers)

void ZygoteLoaderModule::onLoad(zygisk::Api *_api, JNIEnv *_env) {
    api = _api;
    env = _env;

    LOGD("Request dlclose module");
    api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
}

const char *get_package_name(const char *data_dir, const char *process_name) {
    struct stat st; // NOLINT(*-pro-type-member-init)
    // check data dir is accessible for current uid
    if (stat(data_dir, &st) == -1) {
        LOGD("skip injecting into %s - failed to stat data directory", process_name);
        return nullptr;
    }
    const char *last_slash = strrchr(data_dir, '/');
    const char *package_name;
    if (last_slash != nullptr) {
        package_name = last_slash + 1;
        LOGD("Package name: %s for process: %s", package_name, process_name);
    } else {
        LOGD("Failed to parse package name from app_data_dir: %s", data_dir);
        return nullptr;
    }
    return package_name;
}

void ZygoteLoaderModule::preAppSpecialize(zygisk::AppSpecializeArgs *args) {
    RAIIStr process_name = get_string_data(env, args->nice_name);
    RAIIStr data_dir = get_string_data(env, args->app_data_dir);

    if (!process_name || !data_dir) {
        LOGD("skip injecting into %d because its process_name or app_data_dir is null", args->uid);
        return;
    }

    const char *package_name = get_package_name(data_dir, process_name);
    if (!package_name) {
        return;
    }

    RAIIFD module_dir = api->getModuleDir(); // keep alive during preSpecialize
    tryLoadDex(module_dir, package_name, process_name);
    callJavaPreSpecialize();
}

void ZygoteLoaderModule::postAppSpecialize(const zygisk::AppSpecializeArgs *args) {
    callJavaPostSpecialize();
}

void ZygoteLoaderModule::preServerSpecialize(zygisk::ServerSpecializeArgs *args) {
    RAIIFD module_dir = api->getModuleDir(); // keep alive during preSpecialize
    tryLoadDex(module_dir, PACKAGE_SYSTEM_SERVER, PROCESS_SYSTEM_SERVER);
    callJavaPreSpecialize();
}

void ZygoteLoaderModule::postServerSpecialize(const zygisk::ServerSpecializeArgs *args) {
    callJavaPostSpecialize();
}

bool testPackage(int packages_dir, const char *name) {
    return faccessat(packages_dir, name, F_OK, 0) == 0;
}

bool shouldEnable(int module_dir, const char *package_name) {
    RAIIFD<true> packages_dir = openat(module_dir, "packages", O_PATH | O_DIRECTORY);
    if (!packages_dir.isValid()) return false;
    return testPackage(packages_dir, package_name) ^
           testPackage(packages_dir, ALL_PACKAGES);
}

template<typename F>
jsize open_files(int dirfd, RAIILink<RAIIFile> *files, F filter = [](auto) { return true; }) {
    RAIILink<RAIIFile> *current = files;
    RAIILink<RAIIFile> *prev = nullptr;

    RAIIDir dir(dirfd);
    struct dirent64 *entry;
    jsize count = 0;
    while ((entry = readdir64(dir)) != nullptr) {
        if (entry->d_type == DT_REG && filter(entry->d_name)) {
            if (prev != nullptr) {
                current = new RAIILink<RAIIFile>();
                prev->next = current;
            }

            fatal_assert(current != nullptr); // always true
            current->value = new RAIIFile(dirfd, entry->d_name);

            prev = current;
            current = nullptr;
            count++;
        }
    }
    return count;
}

void ZygoteLoaderModule::tryLoadDex(
        int module_dir, const char *package_name, const char *process_name) {
    if (!shouldEnable(module_dir, package_name)) {
        return;
    }

    LOGD("Loading in %s", package_name);

    RAIILink<RAIIFile> files;
    jsize count = open_files(module_dir, &files, [](const char *name) {
        return strncmp(name, "classes", 7) == 0 && strstr(name, ".dex") != nullptr;
    });

    RAIIFile entrypoint_file(module_dir, "entrypoint");
    // The file may not end with \0
    RAIIStr entrypoint_name = strndup((char *) entrypoint_file.data, entrypoint_file.length);

    entrypoint = (jclass) env->NewGlobalRef(
            dex_load_and_init(
                    env, module_dir, entrypoint_name,
                    package_name, process_name,
                    &files, count
            )
    );
}

void ZygoteLoaderModule::callJavaPreSpecialize() {
    if (entrypoint != nullptr) {
        call_pre_specialize(env, entrypoint);
    }
}

void ZygoteLoaderModule::callJavaPostSpecialize() {
    if (entrypoint != nullptr) {
        call_post_specialize(env, entrypoint);

        env->DeleteGlobalRef(entrypoint);
        entrypoint = nullptr;
    }
}

REGISTER_ZYGISK_MODULE(ZygoteLoaderModule)
