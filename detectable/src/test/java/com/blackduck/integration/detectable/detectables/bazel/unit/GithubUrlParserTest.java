package com.blackduck.integration.detectable.detectables.bazel.unit;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.parse.GithubUrlParser;

class GithubUrlParserTest {
    private static GithubUrlParser parser;

    @BeforeAll
    static void setup() {
        parser = new GithubUrlParser();
    }

    @Test
    void testGflags() throws MalformedURLException {
        testValidUrl(
            "https://github.com/gflags/gflags/archive/v2.2.2.tar.gz",
            "gflags",
            "gflags",
            "v2.2.2"
        );
    }

    @Test
    void testGlog() throws MalformedURLException {
        testValidUrl(
            "https://github.com/google/glog/archive/v0.4.0.tar.gz",
            "google",
            "glog",
            "v0.4.0"
        );
    }

    @Test
    void testHttp() throws MalformedURLException {
        testValidUrl(
            "http://github.com/gflags/gflags/archive/v2.2.2.tar.gz",
            "gflags",
            "gflags",
            "v2.2.2"
        );
    }

    @Test
    void testZip() throws MalformedURLException {
        testValidUrl(
            "https://github.com/google/glog/archive/v0.4.0.zip",
            "google",
            "glog",
            "v0.4.0"
        );
    }

    @Test
    void testGZip() throws MalformedURLException {
        testValidUrl(
            "https://github.com/google/glog/archive/v0.4.0.gz",
            "google",
            "glog",
            "v0.4.0"
        );
    }

    @Test
    void testSkylib() throws MalformedURLException {
        testValidUrl(
            "https://github.com/bazelbuild/bazel-skylib/archive/2169ae1c374aab4a09aa90e65efe1a3aad4e279b.tar.gz",
            "bazelbuild",
            "bazel-skylib",
            "2169ae1c374aab4a09aa90e65efe1a3aad4e279b"
        );
    }

    @Test
    void testReleasesDownload() throws MalformedURLException {
        testValidUrl(
            "https://github.com/bazelbuild/bazel-skylib/releases/download/1.0.2/bazel-skylib-1.0.2.tar.gz",
            "bazelbuild",
            "bazel-skylib",
            "1.0.2"
        );
    }

    @Test
    void testReleasesDownloadNodeJs() throws MalformedURLException {
        testValidUrl(
            "https://github.com/bazelbuild/rules_nodejs/releases/download/0.37.0/rules_nodejs-0.37.0.tar.gz",
            "bazelbuild",
            "rules_nodejs",
            "0.37.0"
        );
    }

    @Test
    void testShortShaVersion() throws MalformedURLException {
        testValidUrl(
            "https://github.com/bazelbuild/bazel-toolchains/archive/92dd8a7.zip",
            "bazelbuild",
            "bazel-toolchains",
            "92dd8a7"
        );
    }

    @Test
    void testBareGitCloneUrl() {
        // Bare clone URLs like https://github.com/org/repo.git have no path segment after the repo
        // name. parseRepo() must throw MalformedURLException (not StringIndexOutOfBoundsException)
        // so FinalStepTransformGithubUrl can skip them gracefully.
        // Regression test for: "Range [0, -1) out of bounds for length 9" crash on zenoh.git URLs.
        String url = "https://github.com/eclipse-zenoh/zenoh.git";
        try {
            parser.parseRepo(url);
            Assertions.fail("Expected MalformedURLException for bare .git clone URL");
        } catch (MalformedURLException e) {
            // expected — not a GitHub archive/release URL
        }
    }

    @Test
    void testMissingArchive() {
        try {
            parser.parseVersion("https://github.com/google/glog");
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    @Test
    void testUnexpectedArchive() {
        try {
            parser.parseVersion("https://github.com/google/glog/files/v0.4.0.tar.gz");
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    @Test
    void testMissingFilename() {
        try {
            parser.parseVersion("https://github.com/google/glog/archive/");
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    @Test
    void testMissingExtension() {
        try {
            parser.parseVersion("https://github.com/google/glog/archive/v0.4.0");
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    @Test
    void testNonGithub() {
        String url = "https://www.libsdl.org/release/SDL2-2.0.8.zip";
        try {
            parser.parseOrganization(url);
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
        try {
            parser.parseRepo(url);
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
        try {
            parser.parseVersion(url);
            Assertions.fail("Expected MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    private void testValidUrl(String url, String organization, String repo, String version) throws MalformedURLException {
        Assertions.assertEquals(organization, parser.parseOrganization(url));
        Assertions.assertEquals(repo, parser.parseRepo(url));
        Assertions.assertEquals(version, parser.parseVersion(url));
    }
}
