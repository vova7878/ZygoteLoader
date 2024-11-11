#pragma once

#include <stdint.h> // NOLINT(*-deprecated-headers)

typedef void (*properties_for_each_block)(void *ctx, const char *key, const char *value);

void properties_for_each(const void *properties, uint32_t length,
                         void *ctx, properties_for_each_block block);