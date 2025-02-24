package com.blackduck.integration.detect.tool.detector.executable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.BashResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.BazelResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.CondaResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.CpanResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.CpanmResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.DartResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.DockerResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.FlutterResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.GitResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.GoResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.GradleResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.JavaResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.LernaResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.MavenResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.NpmResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.PearResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.PipResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.PipenvResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.PythonResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.Rebar3Resolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.SbtResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.SwiftResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.OpamResolver;
import com.blackduck.integration.detectable.detectables.conan.cli.ConanResolver;

public class DetectExecutableResolver implements
    JavaResolver, GradleResolver, BashResolver, ConanResolver, CondaResolver, CpanmResolver, CpanResolver, DartResolver, PearResolver, Rebar3Resolver, PythonResolver, PipResolver,
    PipenvResolver, MavenResolver, NpmResolver, BazelResolver,
    DockerResolver, GitResolver, SwiftResolver, GoResolver, LernaResolver, SbtResolver, FlutterResolver, OpamResolver {

    private final DirectoryExecutableFinder directoryExecutableFinder;
    private final SystemPathExecutableFinder systemPathExecutableFinder;
    private final DetectExecutableOptions detectExecutableOptions;

    private final Map<String, File> cachedExecutables = new HashMap<>();

    public DetectExecutableResolver(
        DirectoryExecutableFinder directoryExecutableFinder,
        SystemPathExecutableFinder systemPathExecutableFinder,
        DetectExecutableOptions detectExecutableOptions
    ) {
        this.directoryExecutableFinder = directoryExecutableFinder;
        this.systemPathExecutableFinder = systemPathExecutableFinder;
        this.detectExecutableOptions = detectExecutableOptions;
    }

    private File resolve(@Nullable String cacheKey, ExecutableResolverFunction... resolvers) throws DetectableException {
        File resolved = null;
        for (ExecutableResolverFunction resolver : resolvers) {
            resolved = resolver.resolve();
            if (resolved != null) {
                break;
            }
        }
        if (cacheKey != null) {
            cachedExecutables.put(cacheKey, resolved);
        }
        return resolved;
    }

    private File resolveCache(String cacheKey) {
        if (cachedExecutables.containsKey(cacheKey)) {
            return cachedExecutables.get(cacheKey);
        }
        return null;
    }

    private File resolveOverride(Path executableOverride) throws DetectableException {
        if (executableOverride != null) {
            File exe = executableOverride.toFile();
            if (!exe.exists()) {
                throw new DetectableException("Executable override must exist: " + executableOverride);
            } else if (!exe.isFile()) {
                throw new DetectableException("Executable override must be a file: " + executableOverride);
            } else if (!exe.canExecute()) {
                throw new DetectableException("Executable override must be executable: " + executableOverride);
            } else {
                return exe;
            }
        }
        return null;
    }

    private File resolveCachedSystemExecutable(String executableName, Path override) throws DetectableException {
        return resolveCachedSystemExecutable(executableName, executableName, override); //executableName is the cache key
    }

    private File resolveCachedSystemExecutable(String cacheKey, String executableName, Path override) throws DetectableException {
        return resolve(
            cacheKey,
            () -> resolveOverride(override),
            () -> resolveCache(cacheKey),
            () -> systemPathExecutableFinder.findExecutable(executableName)
        );
    }

    private File resolveLocalNonCachedExecutable(String localName, String systemName, DetectableEnvironment environment, Path override) throws DetectableException {
        return resolve(/* not cached */ null,
            () -> resolveOverride(override),
            () -> directoryExecutableFinder.findExecutable(localName, environment.getDirectory()),
            () -> systemPathExecutableFinder.findExecutable(systemName)
        );
    }

    @Override
    public ExecutableTarget resolveBash() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("bash", detectExecutableOptions.getBashUserPath()));
    }

    @Override
    public ExecutableTarget resolveBazel() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("bazel", detectExecutableOptions.getBazelUserPath()));
    }

    @Override
    public ExecutableTarget resolveConda() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("conda", detectExecutableOptions.getCondaUserPath()));
    }

    @Override
    public ExecutableTarget resolveCpan() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("cpan", detectExecutableOptions.getCpanUserPath()));
    }

    @Override
    public ExecutableTarget resolveCpanm() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("cpanm", detectExecutableOptions.getCpanmUserPath()));
    }

    @Override
    public ExecutableTarget resolveGradle(DetectableEnvironment environment) throws DetectableException {
        return ExecutableTarget.forFile(resolveLocalNonCachedExecutable("gradlew", "gradle", environment, detectExecutableOptions.getGradleUserPath()));
    }

    @Override
    public ExecutableTarget resolveMaven(DetectableEnvironment environment) throws DetectableException {
        return ExecutableTarget.forFile(resolveLocalNonCachedExecutable("mvnw", "mvn", environment, detectExecutableOptions.getMavenUserPath()));
    }

    @Override
    public ExecutableTarget resolveNpm(DetectableEnvironment environment) throws DetectableException {
        return ExecutableTarget.forFile(resolveLocalNonCachedExecutable("npm", "npm", environment, detectExecutableOptions.getNpmUserPath()));
    }

    @Override
    public ExecutableTarget resolvePear() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("pear", detectExecutableOptions.getPearUserPath()));
    }

    @Override
    public ExecutableTarget resolvePip() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("pip", detectExecutableOptions.getPipUserPath()));
    }

    @Override
    public ExecutableTarget resolvePipenv() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("pipenv", detectExecutableOptions.getPipenvUserPath()));
    }

    @Override
    public ExecutableTarget resolvePython() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("python", detectExecutableOptions.getPythonUserPath()));
    }

    @Override
    public ExecutableTarget resolveRebar3() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("rebar3", detectExecutableOptions.getRebarUserPath()));
    }

    @Override
    public ExecutableTarget resolveJava() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("java", detectExecutableOptions.getJavaUserPath()));
    }

    @Override
    public ExecutableTarget resolveDocker() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("docker", detectExecutableOptions.getDockerUserPath()));
    }
    
    @Override
    public ExecutableTarget resolveGit() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("git", detectExecutableOptions.getGitUserPath()));
    }

    @Override
    public ExecutableTarget resolveSwift() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("swift", detectExecutableOptions.getSwiftUserPath()));
    }

    @Override
    public ExecutableTarget resolveGo() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("go", detectExecutableOptions.getGoUserPath()));
    }

    @Override
    public ExecutableTarget resolveLerna() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("lerna", detectExecutableOptions.getLernaUserPath()));
    }

    @Override
    public ExecutableTarget resolveSbt() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("sbt", detectExecutableOptions.getSbtUserPath()));
    }

    @Override
    public ExecutableTarget resolveConan(DetectableEnvironment environment) throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("conan", detectExecutableOptions.getConanUserPath()));
    }

    @Override
    @Nullable
    public ExecutableTarget resolveDart() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("dart", detectExecutableOptions.getDartUserPath()));
    }

    @Override
    @Nullable
    public ExecutableTarget resolveFlutter() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("flutter", detectExecutableOptions.getFlutterUserPath()));
    }
    @Override
    @Nullable
    public ExecutableTarget resolveOpam() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("opam", detectExecutableOptions.getOpamUserPath()));
    }
}

