package com.blackduck.integration.detectable.detectables.uv;

/**
 * Python package name normalization shared by the uv detectables and their options:
 * lowercase, and every run of ".", "-" and "_" replaced by a single "-".
 * So "lib_a", "Lib.A", "lib--a" and "lib.-a" are all the same name as "lib-a".
 */
public final class UVPackageNameNormalizer {

    private UVPackageNameNormalizer() {
    }

    public static String normalize(String packageName) {
        return packageName.replaceAll("[_.-]+", "-").toLowerCase();
    }
}

