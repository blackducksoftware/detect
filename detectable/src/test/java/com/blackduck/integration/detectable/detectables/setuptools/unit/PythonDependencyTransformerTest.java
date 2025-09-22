package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class PythonDependencyTransformerTest {

    private final PythonDependencyTransformer transformer = new PythonDependencyTransformer();

    static Stream<TestCase> dependencyCases() {
        return Stream.of(
            new TestCase("alembic==1.12.0", "alembic", "1.12.0"),
            new TestCase("darkgraylib>=2.31.0,<3.0", "darkgraylib", "2.31.0"),
            new TestCase("requests>=2.4.0,<3.0.dev0", "requests", "2.4.0"),
            new TestCase("toml>=0.10.0", "toml", "0.10.0"),
            new TestCase("torch @ https://download.pytorch.org/whl/cpu/torch-2.6.0%2Bcpu-cp310-cp310-linux_x86_64.whl", "torch", "2.6.0"),
            new TestCase("torchvision @ https://download.pytorch.org/whl/cpu/torchvision-0.21.0%2Bcpu-cp310-cp310-linux_x86_64.whl", "torchvision", "0.21.0"),
            new TestCase("pip @ https://github.com/pypa/pip/archive/1.3.1.zip", "pip", "1.3.1"),
            new TestCase("flask @ git+https://github.com/pallets/flask.git@2.3.3", "flask", "2.3.3"),
            new TestCase("statsmodels @ git+https://github.com/statsmodels/statsmodels.git@v0.14.0", "statsmodels", "0.14.0")
        );
    }

    @ParameterizedTest
    @MethodSource("dependencyCases")
    void testTransformLine(TestCase testCase) {
        PythonDependency dependency = transformer.transformLine(testCase.line);
        assertEquals(testCase.expectedName, dependency.getName());
        assertEquals(testCase.expectedVersion, dependency.getVersion());
    }

    static class TestCase {
        final String line;
        final String expectedName;
        final String expectedVersion;

        TestCase(String line, String expectedName, String expectedVersion) {
            this.line = line;
            this.expectedName = expectedName;
            this.expectedVersion = expectedVersion;
        }

        @Override
        public String toString() {
            return String.format("line='%s', expectedName=%s, expectedVersion=%s", line, expectedName, expectedVersion);
        }
    }
}
