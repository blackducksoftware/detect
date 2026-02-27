package com.blackduck.integration.detect.tool.detector.executable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.blackduck.integration.detectable.detectable.executable.resolver.*;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.conan.cli.ConanResolver;

public class DetectExecutableResolver implements
    JavaResolver, GradleResolver, BashResolver, ConanResolver, CondaResolver, CpanmResolver, CpanResolver, DartResolver, PearResolver, Rebar3Resolver, PythonResolver, PipResolver,
    PipenvResolver, MavenResolver, NpmResolver, BazelResolver, AntResolver,
    DockerResolver, GitResolver, SwiftResolver, GoResolver, LernaResolver, SbtResolver, FlutterResolver, OpamResolver, CargoResolver, UVResolver {

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
    public ExecutableTarget resolveAnt() throws DetectableException {
        Path override = detectExecutableOptions.getAntUserPath();
        if (override != null) {
            // If user specified a path, use that exact file
            return ExecutableTarget.forFile(resolveOverride(override));
        }

        // On Windows, the DirectoryExecutableFinder searches for extensions in order: .cmd, .bat, .exe
        // So searching for "ant" finds "ant.cmd" first. We want "ant.bat" instead.
        // Solution: Search for the full filename "ant.bat" first (bypasses extension search)
        File antExecutable = systemPathExecutableFinder.findExecutable("ant.bat");

        if (antExecutable == null) {
            // If ant.bat not found, try ant.exe
            antExecutable = systemPathExecutableFinder.findExecutable("ant.exe");
        }

        if (antExecutable == null) {
            // Last resort: try generic "ant" (will find ant.cmd on Windows)
            antExecutable = systemPathExecutableFinder.findExecutable("ant");
        }

        if (antExecutable == null) {
            throw new DetectableException("Could not find ant executable (ant.bat, ant.exe, or ant) in system PATH. Please ensure ANT_HOME/bin is in your PATH.");
        }

        return ExecutableTarget.forFile(antExecutable);
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

    @Override
    public ExecutableTarget resolveCargo(DetectableEnvironment environment) throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("cargo", detectExecutableOptions.getCargoUserPath()));
    }

    @Override
    @Nullable
    public ExecutableTarget resolveUV() throws DetectableException {
        return ExecutableTarget.forFile(resolveCachedSystemExecutable("uv", detectExecutableOptions.getUVUserPath()));
    }
}

