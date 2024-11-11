#pragma once

#include <stdint.h> // NOLINT(*-deprecated-headers)

struct Resource {
    void *base;
    uint32_t length;
};

void resource_map_fd(Resource &resource, int fd);

void resource_release(Resource &resource);
