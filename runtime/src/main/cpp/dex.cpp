#include "dex.hpp"
#include "logger.hpp"
#include <string.h> // NOLINT(*-deprecated-headers)

char *get_string_data(JNIEnv *env, jstring value) {
    const char *str = env->GetStringUTFChars(value, nullptr);
    if (str == nullptr) return nullptr;
    auto out = strdup(str);
    env->ReleaseStringUTFChars(value, str);
    return out;
}

jclass dex_load_and_init(JNIEnv *env, int module_dir, const char *entrypoint_name,
                         const char *package_name, const char *process_name,
                         RAIILink<RAIIFile> *files, jsize dex_count) {

    jclass c_class_loader = env->FindClass("java/lang/ClassLoader");
    fatal_assert(c_class_loader != nullptr);
    jmethodID m_get_system_class_loader = env->GetStaticMethodID(
            c_class_loader,
            "getSystemClassLoader",
            "()Ljava/lang/ClassLoader;"
    );
    fatal_assert(m_get_system_class_loader != nullptr);

    auto o_system_class_loader = env->CallStaticObjectMethod(
            c_class_loader,
            m_get_system_class_loader
    );
    fatal_assert(o_system_class_loader != nullptr);

    jclass c_dex_class_loader = env->FindClass("dalvik/system/InMemoryDexClassLoader");
    fatal_assert(c_dex_class_loader != nullptr);
    jmethodID m_dex_class_loader = env->GetMethodID(
            c_dex_class_loader,
            "<init>",
            "([Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V"
    );
    fatal_assert(m_dex_class_loader != nullptr);

    jclass c_byte_buffer = env->FindClass("java/nio/ByteBuffer");
    fatal_assert(c_byte_buffer != nullptr);
    auto o_buffers = env->NewObjectArray(
            dex_count,
            c_byte_buffer,
            nullptr
    );
    fatal_assert(o_buffers != nullptr);

    for (jsize i = 0; i < dex_count; i++) {
        auto o_buffer = env->NewDirectByteBuffer(
                files->value->data,
                files->value->length
        );
        fatal_assert(o_buffer != nullptr);
        env->SetObjectArrayElement(o_buffers, i, o_buffer);
        files = files->next;
    }

    auto o_dex_class_loader = env->NewObject(
            c_dex_class_loader,
            m_dex_class_loader,
            o_buffers,
            o_system_class_loader
    );
    fatal_assert(o_dex_class_loader != nullptr);

    jmethodID m_load_class = env->GetMethodID(
            c_class_loader,
            "loadClass",
            "(Ljava/lang/String;)Ljava/lang/Class;"
    );
    fatal_assert(m_load_class != nullptr);

    jstring s_entrypoint_name = env->NewStringUTF(entrypoint_name);
    fatal_assert(s_entrypoint_name != nullptr);

    auto c_entrypoint = (jclass) env->CallObjectMethod(
            o_dex_class_loader,
            m_load_class,
            s_entrypoint_name
    );
    fatal_assert(c_entrypoint != nullptr);

    jmethodID m_load = env->GetStaticMethodID(
            c_entrypoint,
            "load",
            "(Ljava/lang/String;Ljava/lang/String;I)Z"
    );
    fatal_assert(m_load != nullptr);
    jstring s_package_name = env->NewStringUTF(package_name);
    fatal_assert(s_package_name != nullptr);
    jstring s_process_name = env->NewStringUTF(process_name);
    fatal_assert(s_process_name != nullptr);

    bool success = env->CallStaticBooleanMethod(
            c_entrypoint,
            m_load,
            s_package_name,
            s_process_name,
            module_dir
    );

    return success ? c_entrypoint : nullptr;
}

void call_pre_specialize(JNIEnv *env, jclass entrypoint) {
    jmethodID m_pre_specialize = env->GetStaticMethodID(
            entrypoint,
            "preSpecialize",
            "()V"
    );
    fatal_assert(m_pre_specialize != nullptr);
    env->CallStaticVoidMethod(entrypoint, m_pre_specialize);
}

void call_post_specialize(JNIEnv *env, jclass entrypoint) {
    jmethodID m_post_specialize = env->GetStaticMethodID(
            entrypoint,
            "postSpecialize",
            "()V"
    );
    fatal_assert(m_post_specialize != nullptr);
    env->CallStaticVoidMethod(entrypoint, m_post_specialize);
}
