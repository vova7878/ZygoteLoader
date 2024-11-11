cmake_minimum_required(VERSION 3.10.2)

project(ZygoteLoader)

set(CMAKE_CXX_STANDARD 17)

set(LINKER_FLAGS "-Wl,--hash-style=both")
set(CXX_FLAGS "-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics")

if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_definitions(-DDEBUG)
    set(CXX_FLAGS "${CXX_FLAGS} -O0")
else ()
    set(CXX_FLAGS "${CXX_FLAGS} -O2 -fvisibility=hidden -fvisibility-inlines-hidden")
    set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections -flto")
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CXX_FLAGS}")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

add_library(zygisk_loader SHARED main.cpp properties.cpp process.cpp dex.cpp resource.cpp)
target_link_libraries(zygisk_loader log)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET zygisk_loader POST_BUILD
            COMMAND ${CMAKE_STRIP} --strip-all --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libzygisk_loader.so")
endif ()