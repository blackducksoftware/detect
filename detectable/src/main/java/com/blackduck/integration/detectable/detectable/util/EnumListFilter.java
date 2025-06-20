package com.blackduck.integration.detectable.detectable.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

public class EnumListFilter<T extends Enum<T>> {
    private final Set<T> excludedSet;

    public static <T extends Enum<T>> EnumListFilter<T> excludeNone() {
        return fromExcluded();
    }

    public static <T extends Enum<T>> EnumListFilter<T> fromExcluded(Set<T> excludedSet) {
        return new EnumListFilter<>(excludedSet);
    }

    public static <T extends Enum<T>> EnumListFilter<T> fromExcluded(List<T> excludedList) {
        return new EnumListFilter<>(new LinkedHashSet<>(excludedList));
    }

    @SafeVarargs
    public static <T extends Enum<T>> EnumListFilter<T> fromExcluded(T... excludedValues) {
        return new EnumListFilter<>(new LinkedHashSet<>(Arrays.asList(excludedValues)));
    }

    private EnumListFilter(Set<T> excludedSet) {
        this.excludedSet = excludedSet;
    }

    public <D> void ifShouldInclude(T enumValue, @Nullable D nullableObject, Consumer<D> reporter) {
        if (nullableObject != null && shouldInclude(enumValue)) {
            reporter.accept(nullableObject);
        }
    }

    public <D> void ifShouldExclude(T enumValue, @Nullable D nullableObject, Consumer<D> reporter) {
        if (nullableObject != null && shouldExclude(enumValue)) {
            reporter.accept(nullableObject);
        }
    }

    public <D> void ifShouldInclude(T enumValue, Optional<D> optionalObject, Consumer<D> reporter) {
        if (optionalObject.isPresent() && shouldInclude(enumValue)) {
            reporter.accept(optionalObject.get());
        }
    }

    public <D> void ifShouldExclude(T enumValue, Optional<D> optionalObject, Consumer<D> reporter) {
        if (optionalObject.isPresent() && shouldExclude(enumValue)) {
            reporter.accept(optionalObject.get());
        }
    }

    public void ifShouldInclude(T enumValue, Runnable runnable) {
        if (shouldInclude(enumValue)) {
            runnable.run();
        }
    }

    public void ifShouldExclude(T enumValue, Runnable runnable) {
        if (shouldExclude(enumValue)) {
            runnable.run();
        }
    }

    // Null values are not include (excluded)
    public <D> boolean shouldInclude(T enumValue, @Nullable D nullableObject) {
        return nullableObject != null && shouldInclude(enumValue);
    }

    // Null values are excluded
    public <D> boolean shouldExclude(T enumValue, @Nullable D nullableObject) {
        return nullableObject == null || shouldExclude(enumValue);
    }

    public boolean shouldInclude(T enumValue) {
        return !shouldExclude(enumValue);
    }

    public boolean shouldExclude(T enumValue) {
        return excludedSet.contains(enumValue);
    }

    public boolean shouldIncludeAll() {
        return excludedSet.isEmpty();
    }
}
