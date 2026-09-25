package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.util.Collections;
import java.util.List;

public class CompatibilityMatrix {
    private int schemaVersion;
    private String matrixSourceUrl;
    private List<CompatibilityRow> rows = Collections.emptyList();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getMatrixSourceUrl() {
        return matrixSourceUrl;
    }

    public List<CompatibilityRow> getRows() {
        return rows;
    }
}
