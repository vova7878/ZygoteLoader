#include "resource.hpp"

#include "logger.hpp"

#include <sys/mman.h>
#include <sys/stat.h>

#include <fcntl.h>
#include <unistd.h>

Resource::Resource(int dirfd, const char *name) {
    int resfd = openat(dirfd, name, O_RDONLY);
    fatal_assert(resfd >= 0);

    struct stat s{};
    fatal_assert(fstat(resfd, &s) >= 0);

    void *data = mmap(nullptr, s.st_size, PROT_READ, MAP_SHARED, resfd, 0);
    fatal_assert(data != MAP_FAILED);

    close(resfd);

    base = data;
    length = s.st_size;
}

Resource::~Resource() {
    fatal_assert(munmap(base, length) >= 0);
}