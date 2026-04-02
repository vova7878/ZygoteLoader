package com.v7878.zygisk.gradle;

import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.variant.DslExtension;

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

        target.getDependencies().add("implementation", BuildConfig.RUNTIME_DEPENDENCY);

        var projectExtension = target.getExtensions()
                .create("zygisk", ZygoteLoaderExtension.class);

        target.getExtensions().configure(ApplicationAndroidComponentsExtension.class, components -> {
            var extension = new DslExtension.Builder("zygisk")
                    .extendBuildTypeWith(ZygoteLoaderExtension.class)
                    .extendProductFlavorWith(ZygoteLoaderExtension.class)
                    .build();

            components.registerExtension(extension, config ->
                    ZygoteLoaderExtension.merge(
                            projectExtension,
                            config.buildTypeExtension(ZygoteLoaderExtension.class),
                            config.productFlavorsExtensions(ZygoteLoaderExtension.class)
                    )
            );

            components.onVariants(components.selector().all(), variant -> {
                var decorator = new ZygoteLoaderDecorator(target);
                decorator.initExtension(variant);
                target.afterEvaluate(unused -> decorator.decorateVariant(variant));
            });
        });
    }
}
