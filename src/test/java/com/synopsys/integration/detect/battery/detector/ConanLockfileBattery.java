package com.synopsys.integration.detect.battery.detector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detect.battery.util.DetectorBatteryTest;

@Tag("battery")
public class ConanLockfileBattery {

    @Test
    void conanLockShortFormExternalId() {
        DetectorBatteryTest test = new DetectorBatteryTest("conan-lock-shortform", "conan-lock/shortform");
        test.sourceDirectoryNamed("conan-lock");
        test.sourceFileFromResource("conan.lock");
        test.property("detect.conan.attempt.package.revision.match", "false");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void conanLockLongFormExternalId() {
        DetectorBatteryTest test = new DetectorBatteryTest("conan-lock-longform", "conan-lock/longform");
        test.sourceDirectoryNamed("conan-lock");
        test.sourceFileFromResource("conan.lock");
        test.property("detect.conan.attempt.package.revision.match", "true");
        test.expectBdioResources();
        test.run();
    }
}
