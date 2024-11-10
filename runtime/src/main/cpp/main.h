#pragma once

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

struct Resource {
    void *base;
    uint32_t length;
};

struct Resource *resource_map_fd(int fd);

void resource_release(struct Resource *resource);

#ifdef __cplusplus
};
#endif
