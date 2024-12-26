package com.v7878.zygisk.gradle;

import com.android.build.api.variant.VariantExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ZygoteLoaderExtension implements VariantExtension {
    private final Set<String> packages = new HashSet<>();

    @Nonnull
    public Set<String> getPackages() {
        return packages;
    }

    public void packages(@Nonnull String... pkgs) {
        packages.addAll(List.of(pkgs));
    }

    private String id;
    private String name;
    private String author;
    private String description;
    private String entrypoint;
    private String archiveName;
    private String updateJson;

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getAuthor() {
        return author;
    }

    public void setAuthor(@Nullable String author) {
        this.author = author;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(@Nullable String entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Nullable
    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(@Nullable String archiveName) {
        this.archiveName = archiveName;
    }

    @Nullable
    public String getUpdateJson() {
        return updateJson;
    }

    public void setUpdateJson(@Nullable String updateJson) {
        this.updateJson = updateJson;
    }
}
