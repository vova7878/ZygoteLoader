package com.v7878.zygisk.gradle;

import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

public class ZygoteLoaderPlugin implements Plugin<Project> {
    @Override
    public void apply(@Nonnull Project target) {
        if (!target.getPlugins().hasPlugin("com.android.application")) {
            throw new GradleException("com.android.application not applied");
        }

        ZygoteLoaderExtension extension = target.getExtensions()
                .create("zygisk", ZygoteLoaderExtension.class);

        target.getDependencies().add("implementation", BuildConfig.RUNTIME_DEPENDENCY);

        //TODO
        target.getExtensions().getByType(ApplicationExtension.class)
                .getDefaultConfig().setMultiDexEnabled(false);

        target.getExtensions().configure(ApplicationAndroidComponentsExtension.class, components -> {
            ZygoteLoaderDecorator decorator = new ZygoteLoaderDecorator(target, extension);
            components.onVariants(components.selector().all(), variant -> {
                target.afterEvaluate(prj -> decorator.decorateVariant(variant));
            });
        });
    }
}
