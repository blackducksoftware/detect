package com.blackduck.integration.detectable.detectables.bazel.pipeline;

import java.util.Collections;
import java.util.List;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelEnvironmentAnalyzer;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.FinalStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatch pipeline for HTTP archive detection.
 *
 * Design intent: Run both HTTP strategies, but decide per-repo which one applies,
 * using mode only as a capability gate. In practice here we make a pipeline-level
 * decision: attempt the bzlmod pipeline first when allowed; if it fails or produces
 * no dependencies, fallback to the WORKSPACE pipeline. This keeps env detection
 * centralized and avoids duplicated global Bazel calls.
 *
 * Why fallback is needed even with global env detection:
 * - Global environment detection (e.g., checking mod graph availability) tells us
 *   if the Bazel environment is BZLMOD-capable, but for a given target the external
 *   repositories may still be wired via legacy WORKSPACE rules in hybrid repos.
 * - Therefore, bzlmod capability does not guarantee that HTTP dependencies for the
 *   target are resolvable via bzlmod. When bzlmod resolution fails or yields no
 *   results, we treat it as a signal to fallback to the WORKSPACE strategy.
 */
public class HttpArchiveDispatchPipeline extends Pipeline {
    private final Logger logger = LoggerFactory.getLogger(HttpArchiveDispatchPipeline.class);

    private final Pipeline bzlmodPipeline;
    private final Pipeline workspacePipeline;
    private final BazelEnvironmentAnalyzer.Mode mode;

    // Provide a no-op FinalStep because we override run() and do not use Pipeline's normal step execution.
    private static final class NoOpFinalStep implements FinalStep {
        @Override
        public List<Dependency> finish(List<String> pipelineData) {
            return Collections.emptyList();
        }
    }

    public HttpArchiveDispatchPipeline(
        Pipeline bzlmodPipeline,
        Pipeline workspacePipeline,
        BazelEnvironmentAnalyzer.Mode mode
    ) {
        super(Collections.emptyList(), new NoOpFinalStep());
        this.bzlmodPipeline = bzlmodPipeline;
        this.workspacePipeline = workspacePipeline;
        this.mode = mode;
    }

    @Override
    public List<Dependency> run() throws DetectableException, ExecutableFailedException {
        // WORKSPACE-only environment: run workspace pipeline and return its result.
        if (mode == BazelEnvironmentAnalyzer.Mode.WORKSPACE) {
            logger.info("HttpArchiveDispatch: mode=WORKSPACE → running workspace pipeline only");
            return workspacePipeline.run();
        }

        // BZLMOD-capable environment: attempt bzlmod pipeline first.
        try {
            logger.info("HttpArchiveDispatch: mode=BZLMOD → attempting bzlmod pipeline first");
            List<Dependency> bzlmodDeps = bzlmodPipeline.run();
            if (bzlmodDeps != null && !bzlmodDeps.isEmpty()) {
                logger.debug("HttpArchiveDispatch: bzlmod pipeline produced {} deps → returning", bzlmodDeps.size());
                return bzlmodDeps;
            }
            logger.info("HttpArchiveDispatch: bzlmod pipeline returned empty result → fallback to workspace pipeline");
        } catch (ExecutableFailedException | DetectableException e) {
            // Controlled fallback for HTTP probing only. Do not treat as fatal here.
            logger.info("HttpArchiveDispatch: bzlmod pipeline failed, falling back to workspace pipeline: {}", e.getMessage());
            logger.debug("HttpArchiveDispatch: exception", e);
        }

        // Fallback path: run workspace pipeline for hybrid/legacy repos.
        return workspacePipeline.run();
    }
}
