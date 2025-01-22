#pragma once

#include "raii.hpp"
#include "ext/zygisk.hpp"

class ZygoteLoaderModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override;

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override;

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override;

    void preServerSpecialize(zygisk::ServerSpecializeArgs *args) override;

    void postServerSpecialize(const zygisk::ServerSpecializeArgs *args) override;

private:
    void tryLoadDex(const char *package_name);

    void callJavaPreSpecialize();

    void callJavaPostSpecialize();

private:
    zygisk::Api *api = nullptr;
    JNIEnv *env = nullptr;
    jclass entrypoint = nullptr;
};