package com.blackduck.integration.detectable.factory;

import java.io.File;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.BdioTransformer;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.parse.CommandParser;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
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
import com.blackduck.integration.detectable.detectable.inspector.GradleInspectorResolver;
import com.blackduck.integration.detectable.detectable.inspector.PipInspectorResolver;
import com.blackduck.integration.detectable.detectable.inspector.ProjectInspectorResolver;
import com.blackduck.integration.detectable.detectable.inspector.nuget.NugetInspectorResolver;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectable;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.BazelExtractor;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectables.bazel.BazelWorkspaceFileParser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.WorkspaceRuleChooser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeDetectable;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeDetectableOptions;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeExtractor;
import com.blackduck.integration.detectable.detectables.bitbake.collect.BitbakeCommandRunner;
import com.blackduck.integration.detectable.detectables.bitbake.collect.BuildFileFinder;
import com.blackduck.integration.detectable.detectables.bitbake.parse.BitbakeEnvironmentParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.BitbakeRecipesParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.GraphNodeLabelParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.LicenseManifestParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.PwdOutputParser;
import com.blackduck.integration.detectable.detectables.bitbake.transform.BitbakeDependencyGraphTransformer;
import com.blackduck.integration.detectable.detectables.bitbake.transform.BitbakeGraphTransformer;
import com.blackduck.integration.detectable.detectables.cargo.CargoExtractor;
import com.blackduck.integration.detectable.detectables.cargo.CargoLockDetectable;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoDependencyLineParser;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoLockPackageDataTransformer;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoLockPackageTransformer;
import com.blackduck.integration.detectable.detectables.carthage.CarthageExtractor;
import com.blackduck.integration.detectable.detectables.carthage.CarthageLockDetectable;
import com.blackduck.integration.detectable.detectables.carthage.parse.CartfileResolvedParser;
import com.blackduck.integration.detectable.detectables.carthage.transform.CarthageDeclarationTransformer;
import com.blackduck.integration.detectable.detectables.clang.ClangDetectable;
import com.blackduck.integration.detectable.detectables.clang.ClangDetectableOptions;
import com.blackduck.integration.detectable.detectables.clang.ClangExtractor;
import com.blackduck.integration.detectable.detectables.clang.ForgeChooser;
import com.blackduck.integration.detectable.detectables.clang.LinuxDistroToForgeMapper;
import com.blackduck.integration.detectable.detectables.clang.compilecommand.CompileCommandDatabaseParser;
import com.blackduck.integration.detectable.detectables.clang.compilecommand.CompileCommandParser;
import com.blackduck.integration.detectable.detectables.clang.dependencyfile.ClangPackageDetailsTransformer;
import com.blackduck.integration.detectable.detectables.clang.dependencyfile.DependencyFileDetailGenerator;
import com.blackduck.integration.detectable.detectables.clang.dependencyfile.DependencyListFileParser;
import com.blackduck.integration.detectable.detectables.clang.dependencyfile.FilePathGenerator;
import com.blackduck.integration.detectable.detectables.clang.linux.LinuxDistro;
import com.blackduck.integration.detectable.detectables.clang.packagemanager.ClangPackageManagerFactory;
import com.blackduck.integration.detectable.detectables.clang.packagemanager.ClangPackageManagerInfoFactory;
import com.blackduck.integration.detectable.detectables.clang.packagemanager.ClangPackageManagerRunner;
import com.blackduck.integration.detectable.detectables.cocoapods.PodlockDetectable;
import com.blackduck.integration.detectable.detectables.cocoapods.PodlockExtractor;
import com.blackduck.integration.detectable.detectables.cocoapods.parser.PodlockParser;
import com.blackduck.integration.detectable.detectables.conan.ConanCodeLocationGenerator;
import com.blackduck.integration.detectable.detectables.conan.cli.Conan1CliDetectable;
import com.blackduck.integration.detectable.detectables.conan.cli.Conan2CliDetectable;
import com.blackduck.integration.detectable.detectables.conan.cli.ConanCliExtractor;
import com.blackduck.integration.detectable.detectables.conan.cli.ConanResolver;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanCliOptions;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan1.ConanInfoLineAnalyzer;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan1.ConanInfoNodeParser;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan1.ConanInfoParser;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan1.element.NodeElementParser;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2.ConanGraphInfoParser;
import com.blackduck.integration.detectable.detectables.conan.cli.process.ConanCommandRunner;
import com.blackduck.integration.detectable.detectables.conan.lockfile.ConanLockfileDetectable;
import com.blackduck.integration.detectable.detectables.conan.lockfile.ConanLockfileExtractor;
import com.blackduck.integration.detectable.detectables.conan.lockfile.ConanLockfileExtractorOptions;
import com.blackduck.integration.detectable.detectables.conan.lockfile.parser.ConanLockfileParser;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectable;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.conda.CondaCliExtractor;
import com.blackduck.integration.detectable.detectables.conda.parser.CondaDependencyCreator;
import com.blackduck.integration.detectable.detectables.conda.parser.CondaListParser;
import com.blackduck.integration.detectable.detectables.cpan.CpanCliDetectable;
import com.blackduck.integration.detectable.detectables.cpan.CpanCliExtractor;
import com.blackduck.integration.detectable.detectables.cpan.parse.CpanListParser;
import com.blackduck.integration.detectable.detectables.cran.PackratLockDetectable;
import com.blackduck.integration.detectable.detectables.cran.PackratLockExtractor;
import com.blackduck.integration.detectable.detectables.cran.parse.PackratDescriptionFileParser;
import com.blackduck.integration.detectable.detectables.cran.parse.PackratLockFileParser;
import com.blackduck.integration.detectable.detectables.dart.PubSpecYamlNameVersionParser;
import com.blackduck.integration.detectable.detectables.dart.pubdep.DartPubDepDetectable;
import com.blackduck.integration.detectable.detectables.dart.pubdep.DartPubDepsDetectableOptions;
import com.blackduck.integration.detectable.detectables.dart.pubdep.PubDepsExtractor;
import com.blackduck.integration.detectable.detectables.dart.pubdep.PubDepsParser;
import com.blackduck.integration.detectable.detectables.dart.pubspec.DartPubSpecLockDetectable;
import com.blackduck.integration.detectable.detectables.dart.pubspec.PubSpecExtractor;
import com.blackduck.integration.detectable.detectables.dart.pubspec.PubSpecLockParser;
import com.blackduck.integration.detectable.detectables.docker.DockerDetectable;
import com.blackduck.integration.detectable.detectables.docker.DockerDetectableOptions;
import com.blackduck.integration.detectable.detectables.docker.DockerExtractor;
import com.blackduck.integration.detectable.detectables.docker.DockerInspectorResolver;
import com.blackduck.integration.detectable.detectables.docker.ImageIdentifierGenerator;
import com.blackduck.integration.detectable.detectables.docker.parser.DockerInspectorResultsFileParser;
import com.blackduck.integration.detectable.detectables.git.GitCliDetectable;
import com.blackduck.integration.detectable.detectables.git.GitParseDetectable;
import com.blackduck.integration.detectable.detectables.git.GitUrlParser;
import com.blackduck.integration.detectable.detectables.git.cli.GitCliExtractor;
import com.blackduck.integration.detectable.detectables.git.cli.GitCommandRunner;
import com.blackduck.integration.detectable.detectables.git.parsing.GitParseExtractor;
import com.blackduck.integration.detectable.detectables.git.parsing.parse.GitConfigNameVersionTransformer;
import com.blackduck.integration.detectable.detectables.git.parsing.parse.GitConfigNodeTransformer;
import com.blackduck.integration.detectable.detectables.git.parsing.parse.GitFileParser;
import com.blackduck.integration.detectable.detectables.go.godep.GoDepExtractor;
import com.blackduck.integration.detectable.detectables.go.godep.GoDepLockDetectable;
import com.blackduck.integration.detectable.detectables.go.godep.parse.GoLockParser;
import com.blackduck.integration.detectable.detectables.go.gogradle.GoGradleDetectable;
import com.blackduck.integration.detectable.detectables.go.gogradle.GoGradleExtractor;
import com.blackduck.integration.detectable.detectables.go.gogradle.GoGradleLockParser;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCliDetectable;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCliExtractor;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCommandRunner;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoGraphParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoListParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoModWhyParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoVersionParser;
import com.blackduck.integration.detectable.detectables.go.gomod.process.GoModGraphGenerator;
import com.blackduck.integration.detectable.detectables.go.vendor.GoVendorDetectable;
import com.blackduck.integration.detectable.detectables.go.vendor.GoVendorExtractor;
import com.blackduck.integration.detectable.detectables.go.vendr.GoVndrDetectable;
import com.blackduck.integration.detectable.detectables.go.vendr.GoVndrExtractor;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorDetectable;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorExtractor;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorOptions;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleRunner;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportParser;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportTransformer;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleRootMetadataParser;
import com.blackduck.integration.detectable.detectables.gradle.parsing.GradleProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.ivy.IvyParseDetectable;
import com.blackduck.integration.detectable.detectables.ivy.IvyParseExtractor;
import com.blackduck.integration.detectable.detectables.ivy.parse.IvyProjectNameParser;
import com.blackduck.integration.detectable.detectables.lerna.LernaDetectable;
import com.blackduck.integration.detectable.detectables.lerna.LernaExtractor;
import com.blackduck.integration.detectable.detectables.lerna.LernaOptions;
import com.blackduck.integration.detectable.detectables.lerna.LernaPackageDiscoverer;
import com.blackduck.integration.detectable.detectables.lerna.LernaPackager;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractor;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractorOptions;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCodeLocationPackager;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenPomDetectable;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenPomWrapperDetectable;
import com.blackduck.integration.detectable.detectables.maven.parsing.MavenProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.npm.cli.NpmCliDetectable;
import com.blackduck.integration.detectable.detectables.npm.cli.NpmCliExtractor;
import com.blackduck.integration.detectable.detectables.npm.cli.NpmCliExtractorOptions;
import com.blackduck.integration.detectable.detectables.npm.cli.parse.NpmCliParser;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmLockfileExtractor;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmLockfileOptions;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmPackageLockDetectable;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmShrinkwrapDetectable;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockFileProjectIdTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.npm.packagejson.NpmPackageJsonParseDetectable;
import com.blackduck.integration.detectable.detectables.npm.packagejson.NpmPackageJsonParseDetectableOptions;
import com.blackduck.integration.detectable.detectables.npm.packagejson.PackageJsonExtractor;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorExtractor;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetSolutionDetectable;
import com.blackduck.integration.detectable.detectables.nuget.parse.NugetInspectorParser;
import com.blackduck.integration.detectable.detectables.packagist.ComposerLockDetectable;
import com.blackduck.integration.detectable.detectables.packagist.ComposerLockDetectableOptions;
import com.blackduck.integration.detectable.detectables.packagist.ComposerLockExtractor;
import com.blackduck.integration.detectable.detectables.packagist.parse.PackagistParser;
import com.blackduck.integration.detectable.detectables.pear.PearCliDetectable;
import com.blackduck.integration.detectable.detectables.pear.PearCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.pear.PearCliExtractor;
import com.blackduck.integration.detectable.detectables.pear.parse.PearListParser;
import com.blackduck.integration.detectable.detectables.pear.parse.PearPackageDependenciesParser;
import com.blackduck.integration.detectable.detectables.pear.parse.PearPackageXmlParser;
import com.blackduck.integration.detectable.detectables.pear.transform.PearDependencyGraphTransformer;
import com.blackduck.integration.detectable.detectables.pip.inspector.PipInspectorDetectable;
import com.blackduck.integration.detectable.detectables.pip.inspector.PipInspectorDetectableOptions;
import com.blackduck.integration.detectable.detectables.pip.inspector.PipInspectorExtractor;
import com.blackduck.integration.detectable.detectables.pip.inspector.parser.PipInspectorTreeParser;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDependencyTransformer;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDetectable;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileExtractor;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.PipenvDetectable;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.PipenvDetectableOptions;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.PipenvExtractor;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.parser.PipEnvJsonGraphParser;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.parser.PipenvFreezeParser;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.parser.PipenvTransformer;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockDependencyTransformer;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockDependencyVersionParser;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockDetectable;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockDetectableOptions;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockExtractor;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockTransformer;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockDetectable;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockExtractor;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.poetry.PoetryDetectable;
import com.blackduck.integration.detectable.detectables.poetry.PoetryExtractor;
import com.blackduck.integration.detectable.detectables.poetry.PoetryOptions;
import com.blackduck.integration.detectable.detectables.poetry.parser.PoetryLockParser;
import com.blackduck.integration.detectable.detectables.poetry.parser.ToolPoetrySectionParser;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorExtractor;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorOptions;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorParser;
import com.blackduck.integration.detectable.detectables.rebar.RebarDetectable;
import com.blackduck.integration.detectable.detectables.rebar.RebarExtractor;
import com.blackduck.integration.detectable.detectables.rebar.parse.Rebar3TreeParser;
import com.blackduck.integration.detectable.detectables.rubygems.gemlock.GemlockDetectable;
import com.blackduck.integration.detectable.detectables.rubygems.gemlock.GemlockExtractor;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.GemspecParseDetectable;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.GemspecParseDetectableOptions;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.GemspecParseExtractor;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.parse.GemspecLineParser;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.parse.GemspecParser;
import com.blackduck.integration.detectable.detectables.sbt.SbtDetectable;
import com.blackduck.integration.detectable.detectables.sbt.SbtDetectableOptions;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtCommandArgumentGenerator;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotExtractor;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotGraphNodeParser;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotOutputParser;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtGraphParserTransformer;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtPluginFinder;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtRootNodeFinder;
import com.blackduck.integration.detectable.detectables.setuptools.SetupToolsExtractor;
import com.blackduck.integration.detectable.detectables.setuptools.tbuild.SetupToolsBuildDetectable;
import com.blackduck.integration.detectable.detectables.setuptools.buildless.SetupToolsBuildlessDetectable;
import com.blackduck.integration.detectable.detectables.setuptools.transform.SetupToolsGraphTransformer;
import com.blackduck.integration.detectable.detectables.swift.cli.SwiftCliDetectable;
import com.blackduck.integration.detectable.detectables.swift.cli.SwiftCliParser;
import com.blackduck.integration.detectable.detectables.swift.cli.SwiftExtractor;
import com.blackduck.integration.detectable.detectables.swift.cli.SwiftPackageTransformer;
import com.blackduck.integration.detectable.detectables.swift.lock.PackageResolvedExtractor;
import com.blackduck.integration.detectable.detectables.swift.lock.SwiftPackageResolvedDetectable;
import com.blackduck.integration.detectable.detectables.swift.lock.parse.PackageResolvedDataChecker;
import com.blackduck.integration.detectable.detectables.swift.lock.parse.PackageResolvedFormatChecker;
import com.blackduck.integration.detectable.detectables.swift.lock.parse.PackageResolvedFormatParser;
import com.blackduck.integration.detectable.detectables.swift.lock.parse.PackageResolvedParser;
import com.blackduck.integration.detectable.detectables.swift.lock.transform.PackageResolvedTransformer;
import com.blackduck.integration.detectable.detectables.xcode.XcodeProjectDetectable;
import com.blackduck.integration.detectable.detectables.xcode.XcodeWorkspaceDetectable;
import com.blackduck.integration.detectable.detectables.xcode.XcodeWorkspaceExtractor;
import com.blackduck.integration.detectable.detectables.xcode.parse.XcodeWorkspaceFormatChecker;
import com.blackduck.integration.detectable.detectables.xcode.parse.XcodeWorkspaceParser;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockDetectable;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockExtractor;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockOptions;
import com.blackduck.integration.detectable.detectables.yarn.YarnPackager;
import com.blackduck.integration.detectable.detectables.yarn.YarnTransformer;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryParser;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.section.YarnLockDependencySpecParser;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.section.YarnLockEntrySectionParserSet;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;
import com.blackduck.integration.detectable.detectables.opam.buildexe.OpamBuildDetectable;
import com.blackduck.integration.detectable.detectables.opam.buildexe.OpamBuildExtractor;
import com.blackduck.integration.detectable.detectables.opam.buildexe.parse.OpamTreeParser;
import com.blackduck.integration.detectable.detectables.opam.lockfile.OpamLockFileDetectable;
import com.blackduck.integration.detectable.detectables.opam.lockfile.OpamLockFileExtractor;
import com.blackduck.integration.detectable.detectables.opam.transform.OpamGraphTransformer;
import com.blackduck.integration.detectable.util.ToolVersionLogger;

/*
 Entry point for creating detectables using most
 */
public class DetectableFactory {

    private final FileFinder fileFinder;
    private final DetectableExecutableRunner executableRunner;
    private final ExternalIdFactory externalIdFactory;
    private final Gson gson;
    private final ToolVersionLogger toolVersionLogger;

    public DetectableFactory(FileFinder fileFinder, DetectableExecutableRunner executableRunner, ExternalIdFactory externalIdFactory, Gson gson) {
        this.fileFinder = fileFinder;
        this.executableRunner = executableRunner;
        this.externalIdFactory = externalIdFactory;
        this.gson = gson;
        this.toolVersionLogger = new ToolVersionLogger(executableRunner);
    }

    //#region Detectables

    //Detectables
    //Should be scoped to Prototype so a new Detectable is created every time one is needed.
    //Should only be accessed through the DetectableFactory.

    public DockerDetectable createDockerDetectable(
        DetectableEnvironment environment,
        DockerDetectableOptions dockerDetectableOptions,
        DockerInspectorResolver dockerInspectorResolver,
        JavaResolver javaResolver,
        DockerResolver dockerResolver
    ) {
        return new DockerDetectable(environment, dockerInspectorResolver, javaResolver, dockerResolver, dockerExtractor(), dockerDetectableOptions);
    }

    public BazelDetectable createBazelDetectable(DetectableEnvironment environment, BazelDetectableOptions bazelDetectableOptions, BazelResolver bazelResolver) {
        return new BazelDetectable(environment, fileFinder, bazelExtractor(bazelDetectableOptions), bazelResolver, bazelDetectableOptions.getTargetName().orElse(null));
    }

    public BitbakeDetectable createBitbakeDetectable(DetectableEnvironment environment, BitbakeDetectableOptions bitbakeDetectableOptions, BashResolver bashResolver) {
        BitbakeExtractor bitbakeExtractor = new BitbakeExtractor(
            toolVersionLogger,
            new BitbakeCommandRunner(executableRunner, bitbakeDetectableOptions.getSourceArguments()),
            new BuildFileFinder(fileFinder, bitbakeDetectableOptions.isFollowSymLinks(), bitbakeDetectableOptions.getSearchDepth()),
            new PwdOutputParser(),
            new BitbakeEnvironmentParser(),
            new BitbakeRecipesParser(),
            new LicenseManifestParser(),
            new BitbakeGraphTransformer(new GraphNodeLabelParser()),
            new BitbakeDependencyGraphTransformer(bitbakeDetectableOptions.getDependencyTypeFilter()),
            bitbakeDetectableOptions.getPackageNames(),
            bitbakeDetectableOptions.getDependencyTypeFilter()
        );
        return new BitbakeDetectable(environment, fileFinder, bitbakeDetectableOptions, bitbakeExtractor, bashResolver);
    }

    public CargoLockDetectable createCargoDetectable(DetectableEnvironment environment) {
        CargoTomlParser cargoTomlParser = new CargoTomlParser();
        CargoDependencyLineParser cargoDependencyLineParser = new CargoDependencyLineParser();
        CargoLockPackageDataTransformer cargoLockPackageDataTransformer = new CargoLockPackageDataTransformer(cargoDependencyLineParser);
        CargoLockPackageTransformer cargoLockPackageTransformer = new CargoLockPackageTransformer();
        CargoExtractor cargoExtractor = new CargoExtractor(cargoTomlParser, cargoLockPackageDataTransformer, cargoLockPackageTransformer);
        return new CargoLockDetectable(environment, fileFinder, cargoExtractor);
    }

    public CarthageLockDetectable createCarthageDetectable(DetectableEnvironment environment) {
        CartfileResolvedParser cartfileResolvedParser = new CartfileResolvedParser();
        CarthageDeclarationTransformer carthageDeclarationTransformer = new CarthageDeclarationTransformer();
        CarthageExtractor carthageExtractor = new CarthageExtractor(cartfileResolvedParser, carthageDeclarationTransformer);
        return new CarthageLockDetectable(environment, fileFinder, carthageExtractor);
    }

    public ClangDetectable createClangDetectable(DetectableEnvironment environment, ClangDetectableOptions clangDetectableOptions) {
        return new ClangDetectable(
            environment,
            executableRunner,
            fileFinder,
            clangPackageManagerFactory().createPackageManagers(),
            clangExtractor(),
            clangDetectableOptions,
            clangPackageManagerRunner()
        );
    }

    public ComposerLockDetectable createComposerDetectable(DetectableEnvironment environment, ComposerLockDetectableOptions composerLockDetectableOptions) {
        PackagistParser packagistParser = new PackagistParser(externalIdFactory, composerLockDetectableOptions.getDependencyTypeFilter());
        ComposerLockExtractor composerLockExtractor = new ComposerLockExtractor(packagistParser);
        return new ComposerLockDetectable(environment, fileFinder, composerLockExtractor);
    }

    public CondaCliDetectable createCondaCliDetectable(DetectableEnvironment environment, CondaResolver condaResolver, CondaCliDetectableOptions condaCliDetectableOptions) {
        return new CondaCliDetectable(environment, fileFinder, condaResolver, condaCliExtractor(), condaCliDetectableOptions);
    }

    public CpanCliDetectable createCpanCliDetectable(DetectableEnvironment environment, CpanResolver cpanResolver, CpanmResolver cpanmResolver) {
        return new CpanCliDetectable(environment, fileFinder, cpanResolver, cpanmResolver, cpanCliExtractor());
    }

    public DartPubSpecLockDetectable createDartPubSpecLockDetectable(DetectableEnvironment environment) {
        PubSpecLockParser pubSpecLockParser = new PubSpecLockParser();
        PubSpecYamlNameVersionParser pubSpecYamlNameVersionParser = new PubSpecYamlNameVersionParser();
        PubSpecExtractor pubSpecExtractor = new PubSpecExtractor(pubSpecLockParser, pubSpecYamlNameVersionParser);
        return new DartPubSpecLockDetectable(environment, fileFinder, pubSpecExtractor);
    }

    public DartPubDepDetectable createDartPubDepDetectable(
        DetectableEnvironment environment,
        DartPubDepsDetectableOptions dartPubDepsDetectableOptions,
        DartResolver dartResolver,
        FlutterResolver flutterResolver
    ) {
        PubDepsParser pubDepsParser = new PubDepsParser();
        PubSpecYamlNameVersionParser pubSpecYamlNameVersionParser = new PubSpecYamlNameVersionParser();
        PubDepsExtractor pubDepsExtractor = new PubDepsExtractor(executableRunner, pubDepsParser, pubSpecYamlNameVersionParser, toolVersionLogger);
        return new DartPubDepDetectable(environment, fileFinder, pubDepsExtractor, dartPubDepsDetectableOptions, dartResolver, flutterResolver);
    }

    public GemlockDetectable createGemlockDetectable(DetectableEnvironment environment) {
        return new GemlockDetectable(environment, fileFinder, gemlockExtractor());
    }

    public GitCliDetectable createGitDetectable(DetectableEnvironment environment, GitResolver gitResolver) {
        return new GitCliDetectable(environment, fileFinder, gitCliExtractor(), gitResolver);
    }

    public GitParseDetectable createGitParseDetectable(DetectableEnvironment environment) {
        return new GitParseDetectable(environment, fileFinder, gitParseExtractor());
    }

    public GoModCliDetectable createGoModCliDetectable(DetectableEnvironment environment, GoResolver goResolver, GoModCliDetectableOptions options) {
        return new GoModCliDetectable(environment, fileFinder, goResolver, goModCliExtractor(options));
    }

    public GoDepLockDetectable createGoLockDetectable(DetectableEnvironment environment) {
        GoLockParser goLockParser = new GoLockParser();
        GoDepExtractor goDepExtractor = new GoDepExtractor(goLockParser);
        return new GoDepLockDetectable(environment, fileFinder, goDepExtractor);
    }

    public GoVndrDetectable createGoVndrDetectable(DetectableEnvironment environment) {
        return new GoVndrDetectable(environment, fileFinder, goVndrExtractor());
    }

    public GoVendorDetectable createGoVendorDetectable(DetectableEnvironment environment) {
        return new GoVendorDetectable(environment, fileFinder, goVendorExtractor());
    }

    public GoGradleDetectable createGoGradleDetectable(DetectableEnvironment environment) {
        return new GoGradleDetectable(environment, fileFinder, goGradleExtractor());
    }

    public GradleInspectorDetectable createGradleDetectable(
        DetectableEnvironment environment,
        GradleInspectorOptions gradleInspectorOptions,
        GradleInspectorResolver gradleInspectorResolver,
        GradleResolver gradleResolver
    ) {
        return new GradleInspectorDetectable(
            environment,
            fileFinder,
            gradleResolver,
            gradleInspectorResolver,
            gradleInspectorExtractor(gradleInspectorOptions),
            gradleInspectorOptions
        );
    }

    public GradleProjectInspectorDetectable createMavenGradleInspectorDetectable(
        DetectableEnvironment detectableEnvironment,
        ProjectInspectorResolver projectInspectorResolver,
        ProjectInspectorOptions projectInspectorOptions
    ) {
        return new GradleProjectInspectorDetectable(detectableEnvironment, fileFinder, projectInspectorResolver, projectInspectorExtractor(), projectInspectorOptions);
    }

    public GemspecParseDetectable createGemspecParseDetectable(DetectableEnvironment environment, GemspecParseDetectableOptions gemspecOptions) {
        GemspecLineParser gemspecLineParser = new GemspecLineParser();
        GemspecParser gemspecParser = new GemspecParser(gemspecLineParser, gemspecOptions.getDependencyTypeFilter());
        GemspecParseExtractor gemspecParseExtractor = new GemspecParseExtractor(gemspecParser);
        return new GemspecParseDetectable(environment, fileFinder, gemspecParseExtractor);
    }

    public IvyParseDetectable createIvyParseDetectable(DetectableEnvironment environment) {
        return new IvyParseDetectable(environment, fileFinder, ivyParseExtractor());
    }

    public MavenPomDetectable createMavenPomDetectable(DetectableEnvironment environment, MavenResolver mavenResolver, MavenCliExtractorOptions mavenCliExtractorOptions, ProjectInspectorOptions projectInspectorOptions, ProjectInspectorResolver projectInspectorResolver) {
        return new MavenPomDetectable(environment, fileFinder, mavenResolver, mavenCliExtractor(), mavenCliExtractorOptions, createMavenProjectInspectorDetectable(environment, projectInspectorResolver, projectInspectorOptions));
    }

    public MavenPomWrapperDetectable createMavenPomWrapperDetectable(
        DetectableEnvironment environment,
        MavenResolver mavenResolver,
        MavenCliExtractorOptions mavenCliExtractorOptions,
        ProjectInspectorOptions projectInspectorOptions,
        ProjectInspectorResolver projectInspectorResolver
    ) {
        return new MavenPomWrapperDetectable(environment, fileFinder, mavenResolver, mavenCliExtractor(), mavenCliExtractorOptions, createMavenProjectInspectorDetectable(environment, projectInspectorResolver, projectInspectorOptions));
    }

    public MavenProjectInspectorDetectable createMavenProjectInspectorDetectable(
        DetectableEnvironment detectableEnvironment, ProjectInspectorResolver projectInspectorResolver,
        ProjectInspectorOptions projectInspectorOptions
    ) {
        return new MavenProjectInspectorDetectable(
            detectableEnvironment,
            fileFinder,
            projectInspectorResolver,
            projectInspectorExtractor(),
            projectInspectorOptions
        );
    }

    public ConanLockfileDetectable createConanLockfileDetectable(DetectableEnvironment environment, ConanLockfileExtractorOptions conanLockfileExtractorOptions) {
        return new ConanLockfileDetectable(environment, fileFinder, conanLockfileExtractor(conanLockfileExtractorOptions), conanLockfileExtractorOptions);
    }

    public Conan1CliDetectable createConanCliDetectable(DetectableEnvironment environment, ConanResolver conanResolver, ConanCliOptions conanCliOptions) {
        return new Conan1CliDetectable(environment, fileFinder, conanResolver, conanCliExtractor(conanCliOptions));
    }

    public Conan2CliDetectable createConan2CliDetectable(DetectableEnvironment environment, ConanResolver conanResolver, ConanCliOptions conanCliOptions) {
        ConanGraphInfoParser graphInfoParser = new ConanGraphInfoParser(gson, conanCliOptions, externalIdFactory);
        return new Conan2CliDetectable(environment, fileFinder, conanResolver, conanCliExtractor(conanCliOptions), graphInfoParser);
    }

    public NpmCliDetectable createNpmCliDetectable(DetectableEnvironment environment, NpmResolver npmResolver, NpmCliExtractorOptions npmCliExtractorOptions) {
        NpmCliParser npmCliParser = new NpmCliParser(externalIdFactory, npmCliExtractorOptions.getDependencyTypeFilter());
        NpmCliExtractor npmCliExtractor = new NpmCliExtractor(executableRunner, npmCliParser, gson, toolVersionLogger);
        return new NpmCliDetectable(environment, fileFinder, npmResolver, npmCliExtractor, npmCliExtractorOptions);
    }

    public NpmPackageLockDetectable createNpmPackageLockDetectable(DetectableEnvironment environment, NpmLockfileOptions npmLockfileOptions) {
        return new NpmPackageLockDetectable(environment, fileFinder, npmLockfileExtractor(npmLockfileOptions));
    }

    public NugetProjectDetectable createNugetProjectDetectable(
        DetectableEnvironment environment,
        NugetInspectorOptions nugetInspectorOptions,
        NugetInspectorResolver nugetInspectorResolver
    ) {
        return new NugetProjectDetectable(environment, fileFinder, nugetInspectorOptions, nugetInspectorResolver, nugetInspectorExtractor());
    }

    public NpmShrinkwrapDetectable createNpmShrinkwrapDetectable(DetectableEnvironment environment, NpmLockfileOptions npmLockfileOptions) {
        return new NpmShrinkwrapDetectable(environment, fileFinder, npmLockfileExtractor(npmLockfileOptions));
    }

    public NpmPackageJsonParseDetectable createNpmPackageJsonParseDetectable(DetectableEnvironment environment, NpmPackageJsonParseDetectableOptions npmPackageJsonOptions) {
        PackageJsonExtractor packageJsonExtractor = new PackageJsonExtractor(gson, externalIdFactory, npmPackageJsonOptions.getNpmDependencyTypeFilter());
        return new NpmPackageJsonParseDetectable(environment, fileFinder, packageJsonExtractor);
    }

    public NugetSolutionDetectable createNugetSolutionDetectable(
        DetectableEnvironment environment,
        NugetInspectorOptions nugetInspectorOptions,
        NugetInspectorResolver nugetInspectorResolver
    ) {
        return new NugetSolutionDetectable(environment, fileFinder, nugetInspectorResolver, nugetInspectorExtractor(), nugetInspectorOptions);
    }

    public NugetProjectInspectorDetectable createNugetParseDetectable(
        DetectableEnvironment environment,
        ProjectInspectorResolver projectInspectorResolver,
        ProjectInspectorOptions projectInspectorOptions
    ) {
        return new NugetProjectInspectorDetectable(environment, fileFinder, projectInspectorResolver, projectInspectorExtractor(), projectInspectorOptions);
    }

    public PackratLockDetectable createPackratLockDetectable(DetectableEnvironment environment) {
        return new PackratLockDetectable(environment, fileFinder, packratLockExtractor());
    }

    public PearCliDetectable createPearCliDetectable(DetectableEnvironment environment, PearCliDetectableOptions pearCliDetectableOptions, PearResolver pearResolver) {
        PearDependencyGraphTransformer pearDependencyGraphTransformer = new PearDependencyGraphTransformer(externalIdFactory, pearCliDetectableOptions.getDependencyTypeFilter());
        PearPackageXmlParser pearPackageXmlParser = new PearPackageXmlParser();
        PearPackageDependenciesParser pearPackageDependenciesParser = new PearPackageDependenciesParser();
        PearListParser pearListParser = new PearListParser();
        PearCliExtractor pearCliExtractor = new PearCliExtractor(
            externalIdFactory,
            executableRunner,
            pearDependencyGraphTransformer,
            pearPackageXmlParser,
            pearPackageDependenciesParser,
            pearListParser
        );
        return new PearCliDetectable(environment, fileFinder, pearResolver, pearCliExtractor);
    }

    public PipenvDetectable createPipenvDetectable(
        DetectableEnvironment environment,
        PipenvDetectableOptions pipenvDetectableOptions,
        PythonResolver pythonResolver,
        PipenvResolver pipenvResolver
    ) {
        return new PipenvDetectable(environment, pipenvDetectableOptions, fileFinder, pythonResolver, pipenvResolver, pipenvExtractor());
    }

    public PipfileLockDetectable createPipfileLockDetectable(
        DetectableEnvironment environment,
        PipfileLockDetectableOptions pipfileLockDetectableOptions
    ) {
        PipfileLockDependencyVersionParser dependencyVersionParser = new PipfileLockDependencyVersionParser();
        PipfileLockTransformer pipfileLockTransformer = new PipfileLockTransformer(dependencyVersionParser, pipfileLockDetectableOptions.getDependencyTypeFilter());
        PipfileLockDependencyTransformer pipfileLockDependencyTransformer = new PipfileLockDependencyTransformer();
        PipfileLockExtractor pipfileLockExtractor = new PipfileLockExtractor(gson, pipfileLockTransformer, pipfileLockDependencyTransformer);
        return new PipfileLockDetectable(environment, fileFinder, pipfileLockExtractor);
    }

    public PipInspectorDetectable createPipInspectorDetectable(
        DetectableEnvironment environment, PipInspectorDetectableOptions pipInspectorDetectableOptions, PipInspectorResolver pipInspectorResolver,
        PythonResolver pythonResolver,
        PipResolver pipResolver
    ) {
        return new PipInspectorDetectable(environment, fileFinder, pythonResolver, pipResolver, pipInspectorResolver, pipInspectorExtractor(), pipInspectorDetectableOptions);
    }

    public RequirementsFileDetectable createRequirementsFileDetectable(
        DetectableEnvironment environment,
        RequirementsFileDetectableOptions requirementsFileDetectableOptions
    ) {
        PythonDependencyTransformer requirementsFileTransformer = new PythonDependencyTransformer();
        RequirementsFileDependencyTransformer requirementsFileDependencyTransformer = new RequirementsFileDependencyTransformer();
        RequirementsFileExtractor requirementsFileExtractor = new RequirementsFileExtractor(requirementsFileTransformer, requirementsFileDependencyTransformer);
        return new RequirementsFileDetectable(environment, fileFinder, requirementsFileExtractor, requirementsFileDetectableOptions);
    }

    public PnpmLockDetectable createPnpmLockDetectable(DetectableEnvironment environment, PnpmLockOptions pnpmLockOptions) {
        PnpmLockYamlParserInitial pnpmLockYamlParser = new PnpmLockYamlParserInitial(pnpmLockOptions);
        PnpmLockExtractor pnpmLockExtractor = new PnpmLockExtractor(pnpmLockYamlParser, packageJsonFiles());
        return new PnpmLockDetectable(environment, fileFinder, pnpmLockExtractor, packageJsonFiles());
    }

    public PodlockDetectable createPodLockDetectable(DetectableEnvironment environment) {
        return new PodlockDetectable(environment, fileFinder, podlockExtractor());
    }

    public PoetryDetectable createPoetryDetectable(DetectableEnvironment environment, PoetryOptions poetryOptions) {
        return new PoetryDetectable(environment, fileFinder, poetryExtractor(), toolPoetrySectionParser(), poetryOptions);
    }

    public RebarDetectable createRebarDetectable(DetectableEnvironment environment, Rebar3Resolver rebar3Resolver) {
        return new RebarDetectable(environment, fileFinder, rebar3Resolver, rebarExtractor());
    }

    public SbtDetectable createSbtDetectable(DetectableEnvironment environment, SbtResolver sbtResolver, SbtDetectableOptions sbtDetectableOptions) {
        return new SbtDetectable(environment, fileFinder, sbtDetectableOptions.getSbtCommandAdditionalArguments(), sbtResolver, sbtDotExtractor(), sbtPluginFinder());
    }

    public SwiftCliDetectable createSwiftCliDetectable(DetectableEnvironment environment, SwiftResolver swiftResolver) {
        return new SwiftCliDetectable(environment, fileFinder, swiftExtractor(), swiftResolver);
    }

    public SwiftPackageResolvedDetectable createSwiftPackageResolvedDetectable(DetectableEnvironment environment) {
        PackageResolvedExtractor packageResolvedExtractor = createPackageResolvedExtractor();
        return new SwiftPackageResolvedDetectable(environment, fileFinder, packageResolvedExtractor);
    }

    public YarnLockDetectable createYarnLockDetectable(DetectableEnvironment environment, YarnLockOptions yarnLockOptions) {
        return new YarnLockDetectable(environment, fileFinder, yarnLockExtractor(yarnLockOptions));
    }

    public LernaDetectable createLernaDetectable(
        DetectableEnvironment environment,
        LernaResolver lernaResolver,
        NpmLockfileOptions npmLockfileOptions,
        LernaOptions lernaOptions,
        YarnLockOptions yarnLockOptions
    ) {
        LernaPackageDiscoverer lernaPackageDiscoverer = new LernaPackageDiscoverer(executableRunner, gson, lernaOptions.getExcludedPackages(), lernaOptions.getIncludedPackages());
        LernaPackager lernaPackager = new LernaPackager(
            fileFinder,
            packageJsonReader(),
            yarnLockParser(),
            npmLockfilePackager(npmLockfileOptions),
            yarnPackager(yarnLockOptions),
            lernaOptions.getLernaPackageTypeFilter()
        );
        LernaExtractor lernaExtractor = new LernaExtractor(lernaPackageDiscoverer, lernaPackager);
        return new LernaDetectable(environment, fileFinder, lernaResolver, lernaExtractor);
    }

    public XcodeProjectDetectable createXcodeProjectDetectable(DetectableEnvironment environment) {
        PackageResolvedExtractor packageResolvedExtractor = createPackageResolvedExtractor();
        return new XcodeProjectDetectable(environment, fileFinder, packageResolvedExtractor);
    }

    public XcodeWorkspaceDetectable createXcodeWorkspaceDetectable(DetectableEnvironment environment) {
        PackageResolvedExtractor packageResolvedExtractor = createPackageResolvedExtractor();
        XcodeWorkspaceParser xcodeWorkspaceParser = new XcodeWorkspaceParser();
        XcodeWorkspaceFormatChecker xcodeWorkspaceFormatChecker = new XcodeWorkspaceFormatChecker();
        XcodeWorkspaceExtractor xcodeWorkspaceExtractor = new XcodeWorkspaceExtractor(xcodeWorkspaceParser, xcodeWorkspaceFormatChecker, packageResolvedExtractor, fileFinder);

        return new XcodeWorkspaceDetectable(environment, fileFinder, packageResolvedExtractor, xcodeWorkspaceExtractor);
    }
    
    public SetupToolsBuildDetectable createSetupToolsBuildDetectable(DetectableEnvironment environment, PipResolver pipResolver) {
        return new SetupToolsBuildDetectable(environment, fileFinder, pipResolver, setupToolsExtractor(environment.getDirectory()));
    }
    
    public SetupToolsBuildlessDetectable createSetupToolsBuildlessDetectable(DetectableEnvironment environment, PipResolver pipResolver) {
        return new SetupToolsBuildlessDetectable(environment, fileFinder, setupToolsExtractor(environment.getDirectory()));
    }

    public OpamBuildDetectable createOpamBuildDetectable(DetectableEnvironment environment, OpamResolver opamResolver) {
        return new OpamBuildDetectable(environment, fileFinder, opamResolver, opamBuildExtractor(environment.getDirectory()));
    }

    public OpamLockFileDetectable createOpamLockFileDetectable(DetectableEnvironment environment, OpamResolver opamResolver) {
        return new OpamLockFileDetectable(environment, fileFinder, opamLockFileExtractor(environment.getDirectory()));
    }

    // Used by three Detectables
    private PackageResolvedExtractor createPackageResolvedExtractor() {
        PackageResolvedParser parser = new PackageResolvedParser(gson);
        PackageResolvedFormatParser formatParser = new PackageResolvedFormatParser(gson);
        PackageResolvedFormatChecker formatChecker = new PackageResolvedFormatChecker();
        PackageResolvedDataChecker packageResolvedDataChecker = new PackageResolvedDataChecker();
        GitUrlParser gitUrlParser = new GitUrlParser();
        PackageResolvedTransformer transformer = new PackageResolvedTransformer(gitUrlParser);
        return new PackageResolvedExtractor(parser, formatParser, formatChecker, packageResolvedDataChecker, transformer);
    }

    //#endregion

    //#region Utility

    private BazelExtractor bazelExtractor(BazelDetectableOptions bazelDetectableOptions) {
        WorkspaceRuleChooser workspaceRuleChooser = new WorkspaceRuleChooser();
        BazelWorkspaceFileParser bazelWorkspaceFileParser = new BazelWorkspaceFileParser();
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser = new HaskellCabalLibraryJsonProtoParser(gson);
        BazelVariableSubstitutor bazelVariableSubstitutor = new BazelVariableSubstitutor(
            bazelDetectableOptions.getTargetName().orElse(null),
            bazelDetectableOptions.getBazelCqueryAdditionalOptions()
        );
        BazelProjectNameGenerator bazelProjectNameGenerator = new BazelProjectNameGenerator();
        return new BazelExtractor(
            executableRunner,
            externalIdFactory,
            bazelWorkspaceFileParser,
            workspaceRuleChooser,
            toolVersionLogger,
            haskellCabalLibraryJsonProtoParser,
            bazelDetectableOptions.getTargetName().orElse(null),
            bazelDetectableOptions.getWorkspaceRulesFromProperty(),
            bazelVariableSubstitutor,
            bazelProjectNameGenerator
        );
    }

    private FilePathGenerator filePathGenerator() {
        return new FilePathGenerator(executableRunner, compileCommandParser(), dependenyListFileParser());
    }

    private DependencyListFileParser dependenyListFileParser() {
        return new DependencyListFileParser();
    }

    private DependencyFileDetailGenerator dependencyFileDetailGenerator() {
        return new DependencyFileDetailGenerator(filePathGenerator());
    }

    private ClangPackageDetailsTransformer clangPackageDetailsTransformer() {
        return new ClangPackageDetailsTransformer(externalIdFactory);
    }

    private ForgeChooser forgeChooser() {
        LinuxDistroToForgeMapper forgeGenerator = new LinuxDistroToForgeMapper();
        LinuxDistro linuxDistro = new LinuxDistro();
        return new ForgeChooser(forgeGenerator, linuxDistro);
    }

    private CompileCommandDatabaseParser compileCommandDatabaseParser() {
        return new CompileCommandDatabaseParser(gson);
    }

    private ClangExtractor clangExtractor() {
        return new ClangExtractor(executableRunner, dependencyFileDetailGenerator(), clangPackageDetailsTransformer(), compileCommandDatabaseParser(), forgeChooser());
    }

    private PodlockParser podlockParser() {
        return new PodlockParser(externalIdFactory);
    }

    private PodlockExtractor podlockExtractor() {
        return new PodlockExtractor(podlockParser());
    }

    private CondaListParser condaListParser() {
        return new CondaListParser(gson, condaDependencyCreator());
    }

    private CondaDependencyCreator condaDependencyCreator() {
        return new CondaDependencyCreator(externalIdFactory);
    }

    private CondaCliExtractor condaCliExtractor() {
        return new CondaCliExtractor(condaListParser(), executableRunner, toolVersionLogger);
    }

    private CpanListParser cpanListParser() {
        return new CpanListParser(externalIdFactory);
    }

    private CpanCliExtractor cpanCliExtractor() {
        return new CpanCliExtractor(cpanListParser(), executableRunner, toolVersionLogger);
    }

    private PackratLockFileParser packratLockFileParser() {
        return new PackratLockFileParser(externalIdFactory);
    }

    private PackratDescriptionFileParser packratDescriptionFileParser() {
        return new PackratDescriptionFileParser();
    }

    private PackratLockExtractor packratLockExtractor() {
        return new PackratLockExtractor(packratDescriptionFileParser(), packratLockFileParser(), fileFinder);
    }

    private GitFileParser gitFileParser() {
        return new GitFileParser();
    }

    private GitConfigNameVersionTransformer gitConfigNameVersionTransformer() {
        return new GitConfigNameVersionTransformer(gitUrlParser());
    }

    private GitConfigNodeTransformer gitConfigNodeTransformer() {
        return new GitConfigNodeTransformer();
    }

    private GitParseExtractor gitParseExtractor() {
        return new GitParseExtractor(gitFileParser(), gitConfigNameVersionTransformer(), gitConfigNodeTransformer(), gitUrlParser());
    }

    private GitUrlParser gitUrlParser() {
        return new GitUrlParser();
    }

    private GitCliExtractor gitCliExtractor() {
        GitCommandRunner gitCommandRunner = new GitCommandRunner(executableRunner);
        return new GitCliExtractor(gitUrlParser(), toolVersionLogger, gitCommandRunner);
    }

    private GoModCliExtractor goModCliExtractor(GoModCliDetectableOptions options) {
        GoModCommandRunner goModCommandRunner = new GoModCommandRunner(executableRunner);
        GoListParser goListParser = new GoListParser(gson);
        GoGraphParser goGraphParser = new GoGraphParser();
        GoModWhyParser goModWhyParser = new GoModWhyParser();
        GoVersionParser goVersionParser = new GoVersionParser();
        GoModGraphGenerator goModGraphGenerator = new GoModGraphGenerator(externalIdFactory);
        return new GoModCliExtractor(
            goModCommandRunner,
            goListParser,
            goGraphParser,
            goModWhyParser,
            goVersionParser,
            goModGraphGenerator,
            externalIdFactory,
            options.getExcludedDependencyTypes()
        );
    }

    private GoVndrExtractor goVndrExtractor() {
        return new GoVndrExtractor(externalIdFactory);
    }

    private GoVendorExtractor goVendorExtractor() {
        return new GoVendorExtractor(gson, externalIdFactory);
    }

    private GradleReportParser gradleReportParser() {
        return new GradleReportParser();
    }

    private GradleReportTransformer gradleReportTransformer(GradleInspectorOptions gradleInspectorOptions) {
        return new GradleReportTransformer(gradleInspectorOptions.getConfigurationTypeFilter());
    }

    private GradleRootMetadataParser gradleRootMetadataParser() {
        return new GradleRootMetadataParser();
    }

    private IvyParseExtractor ivyParseExtractor() {
        return new IvyParseExtractor(saxParser(), ivyProjectNameParser());
    }

    private IvyProjectNameParser ivyProjectNameParser() {
        return new IvyProjectNameParser(saxParser());
    }

    private Rebar3TreeParser rebar3TreeParser() {
        return new Rebar3TreeParser();
    }

    private RebarExtractor rebarExtractor() {
        return new RebarExtractor(executableRunner, rebar3TreeParser(), toolVersionLogger);
    }

    private MavenCodeLocationPackager mavenCodeLocationPackager() {
        return new MavenCodeLocationPackager(externalIdFactory);
    }

    private MavenCliExtractor mavenCliExtractor() {
        return new MavenCliExtractor(executableRunner, mavenCodeLocationPackager(), commandParser(), toolVersionLogger);
    }

    private CommandParser commandParser() {
        return new CommandParser();
    }

    private CompileCommandParser compileCommandParser() {
        return new CompileCommandParser(commandParser());
    }

    private ConanLockfileExtractor conanLockfileExtractor(ConanLockfileExtractorOptions options) {
        ConanCodeLocationGenerator conanCodeLocationGenerator = new ConanCodeLocationGenerator(options.getDependencyTypeFilter(), options.preferLongFormExternalIds());
        ConanLockfileParser conanLockfileParser = new ConanLockfileParser(gson, conanCodeLocationGenerator, externalIdFactory);
        return new ConanLockfileExtractor(conanLockfileParser);
    }

    private ConanCliExtractor conanCliExtractor(ConanCliOptions options) {
        ConanCommandRunner conanCommandRunner = new ConanCommandRunner(executableRunner, options.getLockfilePath().orElse(null), options.getAdditionalArguments().orElse(null));
        ConanInfoLineAnalyzer conanInfoLineAnalyzer = new ConanInfoLineAnalyzer();
        ConanCodeLocationGenerator conanCodeLocationGenerator = new ConanCodeLocationGenerator(options.getDependencyTypeFilter(), options.preferLongFormExternalIds());
        NodeElementParser nodeElementParser = new NodeElementParser(conanInfoLineAnalyzer);
        ConanInfoNodeParser conanInfoNodeParser = new ConanInfoNodeParser(conanInfoLineAnalyzer, nodeElementParser);
        ConanInfoParser conanInfoParser = new ConanInfoParser(conanInfoNodeParser, conanCodeLocationGenerator, externalIdFactory);
        return new ConanCliExtractor(conanCommandRunner, conanInfoParser, toolVersionLogger);
    }

    private NpmLockfilePackager npmLockfilePackager(NpmLockfileOptions npmLockfileOptions) {
        NpmLockfileGraphTransformer npmLockfileGraphTransformer = new NpmLockfileGraphTransformer(npmLockfileOptions.getNpmDependencyTypeFilter());
        return new NpmLockfilePackager(gson, externalIdFactory, npmLockFileProjectIdTransformer(), npmLockfileGraphTransformer);
    }

    private NpmLockFileProjectIdTransformer npmLockFileProjectIdTransformer() {
        return new NpmLockFileProjectIdTransformer(gson, externalIdFactory);
    }

    private NpmLockfileExtractor npmLockfileExtractor(NpmLockfileOptions npmLockfileOptions) {
        return new NpmLockfileExtractor(npmLockfilePackager(npmLockfileOptions));
    }

    private NugetInspectorParser nugetInspectorParser() {
        return new NugetInspectorParser(gson, externalIdFactory);
    }

    private NugetInspectorExtractor nugetInspectorExtractor() {
        return new NugetInspectorExtractor(nugetInspectorParser(), fileFinder, executableRunner);
    }

    private ProjectInspectorParser projectInspectorParser() {
        return new ProjectInspectorParser(gson, externalIdFactory);
    }

    private ProjectInspectorExtractor projectInspectorExtractor() {
        return new ProjectInspectorExtractor(executableRunner, projectInspectorParser());
    }

    private PipEnvJsonGraphParser pipenvJsonGraphParser() {
        return new PipEnvJsonGraphParser(gson);
    }

    private PipenvFreezeParser pipenvFreezeParser() {
        return new PipenvFreezeParser();
    }

    private PipenvTransformer pipenvTransformer() {
        return new PipenvTransformer(externalIdFactory);
    }

    private PipenvExtractor pipenvExtractor() {
        return new PipenvExtractor(executableRunner, pipenvTransformer(), pipenvFreezeParser(), pipenvJsonGraphParser());
    }

    private PipInspectorTreeParser pipInspectorTreeParser() {
        return new PipInspectorTreeParser(externalIdFactory);
    }

    private PipInspectorExtractor pipInspectorExtractor() {
        return new PipInspectorExtractor(executableRunner, pipInspectorTreeParser(), toolVersionLogger);
    }

    private PoetryExtractor poetryExtractor() {
        return new PoetryExtractor(new PoetryLockParser());
    }

    private ToolPoetrySectionParser toolPoetrySectionParser() {
        return new ToolPoetrySectionParser();
    }

    private GemlockExtractor gemlockExtractor() {
        return new GemlockExtractor(externalIdFactory);
    }

    public SbtPluginFinder sbtPluginFinder() {
        return new SbtPluginFinder(executableRunner, new SbtCommandArgumentGenerator());
    }

    private SbtDotExtractor sbtDotExtractor() {
        return new SbtDotExtractor(
            executableRunner,
            sbtDotOutputParser(),
            sbtProjectMatcher(),
            sbtGraphParserTransformer(),
            sbtDotGraphNodeParser(),
            new SbtCommandArgumentGenerator()
        );
    }

    private SbtDotOutputParser sbtDotOutputParser() {
        return new SbtDotOutputParser();
    }

    private SbtRootNodeFinder sbtProjectMatcher() {
        return new SbtRootNodeFinder(sbtDotGraphNodeParser());
    }

    private SbtDotGraphNodeParser sbtDotGraphNodeParser() {
        return new SbtDotGraphNodeParser(externalIdFactory);
    }

    private SbtGraphParserTransformer sbtGraphParserTransformer() {
        return new SbtGraphParserTransformer(sbtDotGraphNodeParser());
    }

    private YarnLockParser yarnLockParser() {
        YarnLockLineAnalyzer yarnLockLineAnalyzer = new YarnLockLineAnalyzer();
        YarnLockDependencySpecParser yarnLockDependencySpecParser = new YarnLockDependencySpecParser(yarnLockLineAnalyzer);
        YarnLockEntrySectionParserSet yarnLockEntryElementParser = new YarnLockEntrySectionParserSet(yarnLockLineAnalyzer, yarnLockDependencySpecParser);
        YarnLockEntryParser yarnLockEntryParser = new YarnLockEntryParser(yarnLockLineAnalyzer, yarnLockEntryElementParser);
        return new YarnLockParser(yarnLockEntryParser);
    }

    private YarnPackager yarnPackager(YarnLockOptions yarnLockOptions) {
        YarnTransformer yarnTransformer = new YarnTransformer(externalIdFactory, yarnLockOptions.getYarnDependencyTypeFilter());
        return new YarnPackager(yarnTransformer);
    }

    private PackageJsonFiles packageJsonFiles() {
        return new PackageJsonFiles(packageJsonReader());
    }

    private PackageJsonReader packageJsonReader() {
        return new PackageJsonReader(gson);
    }

    private YarnLockExtractor yarnLockExtractor(YarnLockOptions yarnLockOptions) {
        return new YarnLockExtractor(yarnLockParser(), yarnPackager(yarnLockOptions), packageJsonFiles(), yarnLockOptions);
    }

    private ClangPackageManagerInfoFactory clangPackageManagerInfoFactory() {
        return new ClangPackageManagerInfoFactory();
    }

    private ClangPackageManagerFactory clangPackageManagerFactory() {
        return new ClangPackageManagerFactory(clangPackageManagerInfoFactory());
    }

    private ClangPackageManagerRunner clangPackageManagerRunner() {
        return new ClangPackageManagerRunner();
    }

    private GradleRunner gradleRunner() {
        return new GradleRunner(executableRunner);
    }

    private GradleInspectorExtractor gradleInspectorExtractor(GradleInspectorOptions gradleInspectorOptions) {
        return new GradleInspectorExtractor(fileFinder, gradleRunner(), gradleReportParser(), gradleReportTransformer(gradleInspectorOptions), gradleRootMetadataParser(),
            toolVersionLogger
        );
    }

    private DockerExtractor dockerExtractor() {
        return new DockerExtractor(
            fileFinder,
            executableRunner,
            new BdioTransformer(),
            new ExternalIdFactory(),
            gson,
            new DockerInspectorResultsFileParser(gson),
            new ImageIdentifierGenerator()
        );
    }

    private GoGradleLockParser goGradleLockParser() {
        return new GoGradleLockParser(externalIdFactory);
    }

    private GoGradleExtractor goGradleExtractor() {
        return new GoGradleExtractor(goGradleLockParser());
    }

    private SAXParser saxParser() {
        try {
            return SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Unable to create SAX Parser.", e);
        }
    }

    private SwiftCliParser swiftCliParser() {
        return new SwiftCliParser(gson);
    }

    private SwiftPackageTransformer swiftPackageTransformer() {
        GitUrlParser gitUrlParser = new GitUrlParser();
        return new SwiftPackageTransformer(gitUrlParser);
    }

    private SwiftExtractor swiftExtractor() {
        return new SwiftExtractor(executableRunner, swiftCliParser(), swiftPackageTransformer(), toolVersionLogger);
    }
    
    private SetupToolsGraphTransformer setupToolsGraphTransformer(File sourceDirectory) {
        return new SetupToolsGraphTransformer(sourceDirectory, externalIdFactory, executableRunner);
    }
    
    private SetupToolsExtractor setupToolsExtractor(File sourceDirectory) {
        return new SetupToolsExtractor(setupToolsGraphTransformer(sourceDirectory));
    }

    private OpamGraphTransformer opamGraphTransformer(File sourceDirectory) {
        return new OpamGraphTransformer(sourceDirectory,externalIdFactory,executableRunner);
    }

    private OpamTreeParser opamTreeParser(File sourceDirectory) {
        return new OpamTreeParser(gson, sourceDirectory, externalIdFactory);
    }

    private OpamBuildExtractor opamBuildExtractor(File sourceDirectory) {
        return new OpamBuildExtractor(opamGraphTransformer(sourceDirectory), opamTreeParser(sourceDirectory), executableRunner, sourceDirectory);
    }

    private OpamLockFileExtractor opamLockFileExtractor(File sourceDirectory) {
        return new OpamLockFileExtractor(opamGraphTransformer(sourceDirectory));
    }


    //#endregion Utility

}
