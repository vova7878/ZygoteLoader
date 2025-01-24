#pragma once

#include <stdint.h> // NOLINT(*-deprecated-headers)

struct RAIIFD {
    RAIIFD(int value); // NOLINT(*-explicit-constructor)

    ~RAIIFD();

    operator int() const { // NOLINT(*-explicit-constructor)
        return value;
    }

    int value;
};

struct RAIIFile {
    RAIIFile(int dirfd, const char *name);

    ~RAIIFile();

    void *data;
    uint32_t length;
};
