package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.util.Objects;

/**
 * Represents a replacement directive in go.mod file.
 * Format: old_module => new_module
 */
public class GoReplaceDirective {
    private final GoModuleInfo oldModule;
    private final GoModuleInfo newModule;

    public GoReplaceDirective(GoModuleInfo oldModule, GoModuleInfo newModule) {
        this.oldModule = oldModule;
        this.newModule = newModule;
    }

    public GoModuleInfo getOldModule() {
        return oldModule;
    }

    public GoModuleInfo getNewModule() {
        return newModule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoReplaceDirective that = (GoReplaceDirective) o;
        return Objects.equals(oldModule, that.oldModule) && Objects.equals(newModule, that.newModule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldModule, newModule);
    }

    @Override
    public String toString() {
        return "GoReplaceDirective{" +
                "oldModule=" + oldModule +
                ", newModule=" + newModule +
                '}';
    }
}
