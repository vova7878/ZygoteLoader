#include "resource.hpp"

#include "logger.hpp"

#include <sys/mman.h>
#include <sys/stat.h>

void resource_map_fd(Resource &resource, int fd) {
    struct stat s{};
    fatal_assert(fstat(fd, &s) >= 0);

    void *base = mmap(nullptr, s.st_size, PROT_READ, MAP_SHARED, fd, 0);
    fatal_assert(base != MAP_FAILED);

    resource.base = base;
    resource.length = s.st_size;
}

void resource_release(Resource &resource) {
    munmap(resource.base, resource.length);

    resource.base = nullptr;
    resource.length = 0;
}