package com.blackduck.integration.detectable.detectables.bun.lockbinary;

import java.util.List;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;

public class BunLockBinaryParser {
    private final YarnLockParser delegate;

    public BunLockBinaryParser(YarnLockParser delegate) {
        this.delegate = delegate;
    }

    public YarnLock parseBunLockBinary(List<String> yarnFormattedLines) {
        return delegate.parseYarnLock(yarnFormattedLines);
    }
}
