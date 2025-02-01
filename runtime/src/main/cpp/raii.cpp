#include "raii.hpp"

#include "logger.hpp"

#include <sys/mman.h>
#include <sys/stat.h>

#include <fcntl.h>
#include <unistd.h>

RAIIFD::RAIIFD(int fd) {
    fatal_assert(fd >= 0);
    value = fd;
}

RAIIFD::~RAIIFD() {
    fatal_assert(close(value) >= 0);
}

RAIIFile::RAIIFile(int dirfd, const char *name) {
    RAIIFD res(openat(dirfd, name, O_RDONLY));

    struct stat s{};
    fatal_assert(fstat(res.value, &s) >= 0);

    void *base = mmap(nullptr, s.st_size, PROT_READ, MAP_SHARED, res, 0);
    fatal_assert(base != MAP_FAILED);

    data = base;
    length = s.st_size;
}

RAIIFile::~RAIIFile() {
    fatal_assert(munmap(data, length) >= 0);
}