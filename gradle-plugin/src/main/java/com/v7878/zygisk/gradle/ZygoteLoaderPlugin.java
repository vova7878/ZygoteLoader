package com.v7878.zygisk.gradle;

import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.variant.DslExtension;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

@SuppressWarnings("UnstableApiUsage")
public class ZygoteLoaderPlugin implements Plugin<Project> {
    @Override
    public void apply(@Nonnull Project target) {
        if (!target.getPlugins().hasPlugin("com.android.application")) {
            throw new GradleException("com.android.application not applied");
        }

        target.getDependencies().add("implementation", BuildConfig.RUNTIME_DEPENDENCY);

        //TODO
        target.getExtensions().getByType(ApplicationExtension.class)
                .getDefaultConfig().setMultiDexEnabled(false);

        target.getExtensions().configure(ApplicationAndroidComponentsExtension.class, components -> {
            var extension = new DslExtension.Builder("zygisk")
                    .extendProjectWith(ZygoteLoaderExtension.class)
                    .extendBuildTypeWith(ZygoteLoaderExtension.class)
                    .extendProductFlavorWith(ZygoteLoaderExtension.class)
                    .build();

            components.registerExtension(extension, config ->
                    ZygoteLoaderExtension.merge(
                            config.projectExtension(ZygoteLoaderExtension.class),
                            config.buildTypeExtension(ZygoteLoaderExtension.class),
                            config.productFlavorsExtensions(ZygoteLoaderExtension.class)
                    )
            );

            ZygoteLoaderDecorator decorator = new ZygoteLoaderDecorator(target);
            components.onVariants(components.selector().all(), variant -> {
                target.afterEvaluate(prj -> decorator.decorateVariant(variant));
            });
        });
    }
}
