package com.blackduck.integration.detectable.detectables.bun.lockb;

import java.util.List;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;

public class BunLockbParser {
    private final YarnLockParser delegate;

    public BunLockbParser(YarnLockParser delegate) {
        this.delegate = delegate;
    }

    public YarnLock parseBunLockb(List<String> yarnFormattedLines) {
        return delegate.parseYarnLock(yarnFormattedLines);
    }
}
