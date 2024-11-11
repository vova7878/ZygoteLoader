#pragma once

#include "resource.hpp"
#include "ext/zygisk.hpp"

class ZygoteLoaderModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override;

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override;

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override;

    void preServerSpecialize(zygisk::ServerSpecializeArgs *args) override;

    void postServerSpecialize(const zygisk::ServerSpecializeArgs *args) override;

private:
    void initialize();

    bool shouldEnableForPackage(const char *packageName) const;

    void fetchResources();

    void reset();

    void prepareFork();

    void tryLoadDex();

private:
    zygisk::Api *api = nullptr;
    JNIEnv *env = nullptr;

    Resource moduleProp = {};
    Resource classesDex = {};

    char *currentProcessName = nullptr;
};