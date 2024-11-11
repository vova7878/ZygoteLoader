#include "properties.hpp"

#include <malloc.h>
#include <string.h> // NOLINT(*-deprecated-headers)

void properties_for_each(const void *properties, uint32_t length,
                         void *ctx, properties_for_each_block block) {
    char *ptr = (char *) malloc(length + 1);

    memcpy(ptr, properties, length);
    ptr[length] = '\0';

    char *line_status = nullptr;
    char *line = strtok_r(ptr, "\n", &line_status);
    while (line != nullptr) {
        char *split = strchr(line, '=');
        if (split != nullptr) {
            *split = '\0';
            block(ctx, line, split + 1);
        }
        line = strtok_r(nullptr, "\n", &line_status);
    }

    free(ptr);
}
