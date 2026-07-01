package com.blackduck.integration.detectable.detectables.meson.functional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

public class MesonDependencyFileParserTest extends DetectableFunctionalTest {
        public MesonDependencyFileParserTest() throws IOException {
        super("meson");
    }

    @Override
    protected void setup() throws IOException {
        Path root = addDirectory(Paths.get("."));
        Path builddir = addDirectory(Paths.get("builddir/meson-info"));
        addFile(
            root.resolve("meson.build"),
            "project('hello','cpp',default_options: ['cpp_std=c++20', 'default_library=shared'],version : '0.1',)",
            "deps = [dependency('boost'),dependency('libcurl'),]",
            "exe = executable('hello', 'hello.cpp', install : true)"
        );
        addFile(
            builddir.resolve("intro-projectinfo.json"),
            "{\"version\": \"0.1\", \"descriptive_name\": \"hello\", \"subproject_dir\": \"subprojects\", \"subprojects\": []}"
        );
        addFile(builddir.resolve("intro-dependencies.json"),
            "[{\"name\": \"boost\", \"type\": \"system\", \"version\": \"1.83.0\", \"compile_args\": [\"-I/usr/include\", \"-DBOOST_ALL_NO_LIB\"], \"link_args\": [], \"include_directories\": [], \"sources\": [], \"extra_files\": [], \"dependencies\": [], \"depends\": [], \"meson_variables\": []}, {\"name\": \"libcurl\", \"type\": \"pkgconfig\", \"version\": \"8.5.0\", \"compile_args\": [\"-I/usr/include/x86_64-linux-gnu\"], \"link_args\": [\"/usr/lib/x86_64-linux-gnu/libcurl.so\"], \"include_directories\": [], \"sources\": [], \"extra_files\": [], \"dependencies\": [], \"depends\": [], \"meson_variables\": []}]"
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createMesonDetectable(detectableEnvironment);
    }
    
    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(new Forge("/", "Generic"), extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(2);
        graphAssert.hasRootDependency("boost", "1.83.0");
        graphAssert.hasRootDependency("libcurl", "8.5.0");
    }
}
