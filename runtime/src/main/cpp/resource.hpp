#pragma once

#include <stdint.h> // NOLINT(*-deprecated-headers)

struct Resource {
    Resource(int dirfd, const char *name);

    ~Resource();

    void *base;
    uint32_t length;
};
