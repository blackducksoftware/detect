package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.parse.GithubUrlParser;

public class FinalStepTransformGithubUrl implements FinalStep {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GithubUrlParser githubUrlParser;
    private static final String REFS_TAGS_PREFIX = "refs/tags/";

    public FinalStepTransformGithubUrl(GithubUrlParser githubUrlParser) {
        this.githubUrlParser = githubUrlParser;
    }

    @Override
    public List<Dependency> finish(List<String> input) throws DetectableException {
        List<Dependency> dependencies = new ArrayList<>();
        for (String potentialGithubUrl : input) {
            logger.debug("bazel URL (potentially a github URL): {}", potentialGithubUrl);
            try {
                String organization = githubUrlParser.parseOrganization(potentialGithubUrl);
                String repo = githubUrlParser.parseRepo(potentialGithubUrl);
                String version = githubUrlParser.parseVersion(potentialGithubUrl);
                // Normalize GitHub archive ref tags like `refs/tags/<tag>` to `<tag>`
                if (version != null && version.startsWith(REFS_TAGS_PREFIX)) {
                    version = version.substring(REFS_TAGS_PREFIX.length());
                }
                Dependency dep = Dependency.FACTORY.createNameVersionDependency(Forge.GITHUB, organization + "/" + repo, version);
                dependencies.add(dep);
            } catch (MalformedURLException e) {
                logger.debug("URL '{}' does not appear to be a github released artifact location", potentialGithubUrl);
            }
        }
        return dependencies;
    }
}
