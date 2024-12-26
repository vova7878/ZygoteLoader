package com.v7878.zygisk.gradle;

import com.android.build.api.dsl.ApplicationExtension;
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

        //TODO
        target.getExtensions().getByType(ApplicationExtension.class)
                .getDefaultConfig().setMultiDexEnabled(false);

        target.getExtensions().configure(ApplicationAndroidComponentsExtension.class, components -> {
            //noinspection UnstableApiUsage
            components.registerExtension(
                    new DslExtension.Builder("zygisk").extendProductFlavorWith(ZygoteLoaderExtension.class).build(),
                    config -> new ZygoteLoaderExtension()
            );

            //components.beforeVariants(components.selector().all(), variantBuilder -> {
            //    var extensionInstance = new ZygoteLoaderExtension();
            //    variantBuilder.registerExtension(ZygoteLoaderExtension.class, extensionInstance);
            //});

            ZygoteLoaderDecorator decorator = new ZygoteLoaderDecorator(target);
            components.onVariants(components.selector().all(), decorator::decorateVariant);
        });
    }
}
