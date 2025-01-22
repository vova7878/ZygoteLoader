#include "dex.hpp"
#include "logger.hpp"

#define find_class(var_name, name) jclass var_name = env->FindClass(name); fatal_assert((var_name) != NULL)
#define find_static_method(var_name, clazz, name, signature) jmethodID var_name = env->GetStaticMethodID(clazz, name, signature); fatal_assert((var_name) != NULL)
#define find_method(var_name, clazz, name, signature) jmethodID var_name = env->GetMethodID(clazz, name, signature); fatal_assert((var_name) != NULL)
#define new_string(text) env->NewStringUTF(text)

jclass dex_load_and_init(JNIEnv *env, const char *package_name,
                         const void *dex_block, uint32_t dex_length,
                         const void *props_block, uint32_t props_length) {

    find_class(c_class_loader, "java/lang/ClassLoader");
    find_static_method(
            m_get_system_class_loader,
            c_class_loader,
            "getSystemClassLoader",
            "()Ljava/lang/ClassLoader;"
    );

    auto o_system_class_loader = env->CallStaticObjectMethod(
            c_class_loader, m_get_system_class_loader);
    fatal_assert(o_system_class_loader != nullptr);

    find_class(c_dex_class_loader, "dalvik/system/InMemoryDexClassLoader");
    find_method(
            m_dex_class_loader,
            c_dex_class_loader,
            "<init>",
            "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V"
    );

    auto o_dex_class_loader = env->NewObject(
            c_dex_class_loader,
            m_dex_class_loader,
            env->NewDirectByteBuffer((void *) dex_block, dex_length),
            o_system_class_loader
    );
    fatal_assert(o_dex_class_loader != nullptr);

    find_method(
            m_load_class,
            c_class_loader,
            "loadClass",
            "(Ljava/lang/String;)Ljava/lang/Class;"
    );

    auto c_entrypoint = (jclass) env->CallObjectMethod(
            o_dex_class_loader,
            m_load_class,
            new_string("com.v7878.zygisk.EntryPoint")
    );
    fatal_assert(c_entrypoint != nullptr);

    find_static_method(m_load, c_entrypoint, "load", "(Ljava/lang/String;Ljava/nio/ByteBuffer;)V");
    env->CallStaticVoidMethod(
            c_entrypoint,
            m_load,
            new_string(package_name),
            env->NewDirectByteBuffer((void *) props_block, props_length)
    );

    return c_entrypoint;
}

void dex_call_pre_specialize(JNIEnv *env, jclass entrypoint) {
    find_static_method(m_pre_specialize, entrypoint, "preSpecialize", "()V");
    env->CallStaticVoidMethod(entrypoint, m_pre_specialize);
}

void dex_call_post_specialize(JNIEnv *env, jclass entrypoint) {
    find_static_method(m_post_specialize, entrypoint, "postSpecialize", "()V");
    env->CallStaticVoidMethod(entrypoint, m_post_specialize);
}
