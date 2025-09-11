package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;
import org.junit.jupiter.api.Test;

public class PythonDependencyTransformerTest {

    @Test
    public void testTransformLine() {
        PythonDependencyTransformer transformer = new PythonDependencyTransformer();

        // Case 1: Normal dependency with exact version
        PythonDependency alembic = transformer.transformLine("alembic==1.12.0");
        assertEquals("alembic", alembic.getName());
        assertEquals("1.12.0", alembic.getVersion());

        // Case 2: Normal dependency with version range
        PythonDependency darkgraylib = transformer.transformLine("darkgraylib>=2.31.0,<3.0");
        assertEquals("darkgraylib", darkgraylib.getName());
        assertEquals("2.31.0", darkgraylib.getVersion());

        PythonDependency requests = transformer.transformLine("requests>=2.4.0,<3.0.dev0");
        assertEquals("requests", requests.getName());
        assertEquals("2.4.0", requests.getVersion());

        // Case 3: Normal dependency with single version constraint
        PythonDependency toml = transformer.transformLine("toml>=0.10.0");
        assertEquals("toml", toml.getName());
        assertEquals("0.10.0", toml.getVersion());

        // Case 4: Dependency with direct URL (HTTP/HTTPS)
        PythonDependency torch = transformer.transformLine("torch @ https://download.pytorch.org/whl/cpu/torch-2.6.0%2Bcpu-cp310-cp310-linux_x86_64.whl");
        assertEquals("torch", torch.getName());
        assertEquals("2.6.0", torch.getVersion());

        PythonDependency torchvision = transformer.transformLine("torchvision @ https://download.pytorch.org/whl/cpu/torchvision-0.21.0%2Bcpu-cp310-cp310-linux_x86_64.whl");
        assertEquals("torchvision", torchvision.getName());
        assertEquals("0.21.0", torchvision.getVersion());

        // Case 5: Archive dependency
        PythonDependency pip = transformer.transformLine("pip @ https://github.com/pypa/pip/archive/1.3.1.zip");
        assertEquals("pip", pip.getName());
        assertEquals("1.3.1", pip.getVersion());

        // Case 6: Git dependency
        PythonDependency flask = transformer.transformLine("flask @ git+https://github.com/pallets/flask.git@2.3.3");
        assertEquals("flask", flask.getName());
        assertEquals("2.3.3", flask.getVersion());
    }
}