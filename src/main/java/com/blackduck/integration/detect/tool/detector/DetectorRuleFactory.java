package com.blackduck.integration.detect.tool.detector;

import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeDetectable;
import com.blackduck.integration.detectable.detectables.cargo.CargoCliDetectable;
import com.blackduck.integration.detectable.detectables.cargo.CargoLockDetectable;
import com.blackduck.integration.detectable.detectables.carthage.CarthageLockDetectable;
import com.blackduck.integration.detectable.detectables.clang.ClangDetectable;
import com.blackduck.integration.detectable.detectables.cocoapods.PodlockDetectable;
import com.blackduck.integration.detectable.detectables.conan.cli.Conan1CliDetectable;
import com.blackduck.integration.detectable.detectables.conan.cli.Conan2CliDetectable;
import com.blackduck.integration.detectable.detectables.conan.lockfile.ConanLockfileDetectable;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectable;
import com.blackduck.integration.detectable.detectables.cpan.CpanCliDetectable;
import com.blackduck.integration.detectable.detectables.cran.PackratLockDetectable;
import com.blackduck.integration.detectable.detectables.dart.pubdep.DartPubDepDetectable;
import com.blackduck.integration.detectable.detectables.dart.pubspec.DartPubSpecLockDetectable;
import com.blackduck.integration.detectable.detectables.git.GitCliDetectable;
import com.blackduck.integration.detectable.detectables.git.GitParseDetectable;
import com.blackduck.integration.detectable.detectables.go.godep.GoDepLockDetectable;
import com.blackduck.integration.detectable.detectables.go.gogradle.GoGradleDetectable;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCliDetectable;
import com.blackduck.integration.detectable.detectables.go.vendor.GoVendorDetectable;
import com.blackduck.integration.detectable.detectables.go.vendr.GoVndrDetectable;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorDetectable;
import com.blackduck.integration.detectable.detectables.gradle.parsing.GradleProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.ivy.IvyParseDetectable;
import com.blackduck.integration.detectable.detectables.lerna.LernaDetectable;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenPomDetectable;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenPomWrapperDetectable;
import com.blackduck.integration.detectable.detectables.maven.parsing.MavenProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.npm.cli.NpmCliDetectable;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmPackageLockDetectable;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmShrinkwrapDetectable;
import com.blackduck.integration.detectable.detectables.npm.packagejson.NpmPackageJsonParseDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetSolutionDetectable;
import com.blackduck.integration.detectable.detectables.opam.buildexe.OpamBuildDetectable;
import com.blackduck.integration.detectable.detectables.opam.lockfile.OpamLockFileDetectable;
import com.blackduck.integration.detectable.detectables.packagist.ComposerLockDetectable;
import com.blackduck.integration.detectable.detectables.pear.PearCliDetectable;
import com.blackduck.integration.detectable.detectables.pip.inspector.PipInspectorDetectable;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDetectable;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.PipenvDetectable;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockDetectable;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockDetectable;
import com.blackduck.integration.detectable.detectables.poetry.PoetryDetectable;
import com.blackduck.integration.detectable.detectables.rebar.RebarDetectable;
import com.blackduck.integration.detectable.detectables.rubygems.gemlock.GemlockDetectable;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.GemspecParseDetectable;
import com.blackduck.integration.detectable.detectables.sbt.SbtDetectable;
import com.blackduck.integration.detectable.detectables.setuptools.tbuild.SetupToolsBuildDetectable;
import com.blackduck.integration.detectable.detectables.setuptools.buildless.SetupToolsBuildlessDetectable;
import com.blackduck.integration.detectable.detectables.swift.cli.SwiftCliDetectable;
import com.blackduck.integration.detectable.detectables.swift.lock.SwiftPackageResolvedDetectable;
import com.blackduck.integration.detectable.detectables.uv.buildexe.UVBuildDetectable;
import com.blackduck.integration.detectable.detectables.uv.lockfile.UVLockFileDetectable;
import com.blackduck.integration.detectable.detectables.xcode.XcodeProjectDetectable;
import com.blackduck.integration.detectable.detectables.xcode.XcodeWorkspaceDetectable;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockDetectable;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.blackduck.integration.detector.rule.builder.DetectorRuleSetBuilder;

public class DetectorRuleFactory {

    //TODO (8.0.0): Decide if things called lock should use the lock default or not.
    public DetectorRuleSet createRules(DetectDetectableFactory detectableFactory) {
        DetectorRuleSetBuilder rules = new DetectorRuleSetBuilder(detectableFactory);

        rules.addDetector(DetectorType.CARGO, detector -> {
            detector.entryPoint(CargoCliDetectable.class)
                .search().defaults();
            detector.entryPoint(CargoLockDetectable.class)
                .search().defaults();
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.CARTHAGE, detector -> {
            detector.entryPoint(CarthageLockDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.COCOAPODS, detector -> {
            detector.entryPoint(PodlockDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.BITBAKE, detector -> {
            detector.entryPoint(BitbakeDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.XCODE, detector -> {
            detector.entryPoint(XcodeWorkspaceDetectable.class)
                .search().defaults();
            detector.entryPoint(XcodeProjectDetectable.class)
                .search()
                .noMaxDepth()
                .nestable()
                .notNestableBeneath(XcodeWorkspaceDetectable.class)
                .notNestableBeneath(XcodeProjectDetectable.class);
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.SWIFT, detector -> {
            detector.entryPoint(SwiftPackageResolvedDetectable.class)
                .search().defaults().nestableExceptTo(DetectorType.XCODE);
            detector.entryPoint(SwiftCliDetectable.class)
                .search().defaults().nestableExceptTo(DetectorType.XCODE);
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.CONAN, detector -> {
            detector.entryPoint(Conan2CliDetectable.class)
                .search().defaults();
            detector.entryPoint(ConanLockfileDetectable.class)
                .search().defaults();
            detector.entryPoint(Conan1CliDetectable.class)
                .search().defaults();
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.CONDA, detector -> {
            detector.entryPoint(CondaCliDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.CPAN, detector -> {
            detector.entryPoint(CpanCliDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.CRAN, detector -> {
            detector.entryPoint(PackratLockDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.PACKAGIST, detector -> {
            detector.entryPoint(ComposerLockDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.DART, detector -> {
            detector.entryPoint(DartPubDepDetectable.class)
                .search().defaults();
            detector.entryPoint(DartPubSpecLockDetectable.class)
                .search().defaults();
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.GO_MOD, detector -> {
            detector.entryPoint(GoModCliDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.GO_DEP, detector -> {
            detector.entryPoint(GoDepLockDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.GO_VNDR, detector -> {
            detector.entryPoint(GoVndrDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.GO_VENDOR, detector -> {
            detector.entryPoint(GoVendorDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.GO_GRADLE, detector -> {
            detector.entryPoint(GoGradleDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.GRADLE, detector -> {
            detector.entryPoint(GradleInspectorDetectable.class)
                .fallback(GradleProjectInspectorDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.IVY, detector -> {
            detector.entryPoint(IvyParseDetectable.class)
                .search().defaultLock();
        });

        rules.addDetector(DetectorType.HEX, detector -> {
            detector.entryPoint(RebarDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.MAVEN, detector -> {
            detector.entryPoint(MavenPomDetectable.class)
                .fallback(MavenProjectInspectorDetectable.class)
                .search().defaults();

            detector.entryPoint(MavenPomWrapperDetectable.class)
                .fallback(MavenProjectInspectorDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.LERNA, detector -> {
            detector.entryPoint(LernaDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.YARN, detector -> {
            detector.entryPoint(YarnLockDetectable.class)
                .search().defaultLock();
        }).yieldsTo(DetectorType.LERNA);

        rules.addDetector(DetectorType.PNPM, detector -> {
            detector.entryPoint(PnpmLockDetectable.class)
                .search().defaultLock();
        }).yieldsTo(DetectorType.LERNA);

        rules.addDetector(DetectorType.NPM, detector -> {
                detector.entryPoint(NpmShrinkwrapDetectable.class)
                    .search().defaultLock();
                detector.entryPoint(NpmPackageLockDetectable.class)
                    .search().defaultLock();
                detector.entryPoint(NpmCliDetectable.class)
                    .search().defaults();
                detector.entryPoint(NpmPackageJsonParseDetectable.class)
                    .search().defaults(); //maybe this one should be defaultLock?
            }).allEntryPointsFallbackToNext()
            .yieldsTo(DetectorType.LERNA, DetectorType.YARN, DetectorType.PNPM);

        rules.addDetector(DetectorType.NUGET, detector -> {
            //four different detectables, last one will be the project inspector
            detector.entryPoint(NugetSolutionDetectable.class)
                .fallback(NugetProjectInspectorDetectable.class)
                .search().defaults();

            detector.entryPoint(NugetProjectDetectable.class)
                .fallback(NugetProjectInspectorDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.SETUPTOOLS, detector -> {
            detector.entryPoint(SetupToolsBuildDetectable.class)
                .search().defaults();
            detector.entryPoint(SetupToolsBuildlessDetectable.class)
                .search().defaults();
         }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.POETRY, detector -> {
            detector.entryPoint(PoetryDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.UV, detector -> {
            detector.entryPoint(UVBuildDetectable.class)
                    .search().defaults();
            detector.entryPoint(UVLockFileDetectable.class)
                    .search().defaults();
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.PIP, detector -> {
                detector.entryPoint(PipenvDetectable.class)
                    .search().defaults();
                detector.entryPoint(PipInspectorDetectable.class)
                    .search().defaults();
                detector.entryPoint(PipfileLockDetectable.class)
                    .search().defaults();
                detector.entryPoint(RequirementsFileDetectable.class)
                    .search().defaults();
            })
            .allEntryPointsFallbackToNext()
            .yieldsTo(DetectorType.POETRY)
            .yieldsTo(DetectorType.UV);

        rules.addDetector(DetectorType.RUBYGEMS, detector -> {
            detector.entryPoint(GemlockDetectable.class)
                .search().defaults();//should this be default lock?
            detector.entryPoint(GemspecParseDetectable.class)
                .search().defaults();
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.GIT, detector -> {
            detector.entryPoint(GitCliDetectable.class)
                .search().defaults();
            detector.entryPoint(GitParseDetectable.class)
                .search().defaults();
        }).allEntryPointsFallbackToNext();

        rules.addDetector(DetectorType.SBT, detector -> {
            detector.entryPoint(SbtDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.PEAR, detector -> {
            detector.entryPoint(PearCliDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.CLANG, detector -> {
            detector.entryPoint(ClangDetectable.class)
                .search().defaults();
        });

        rules.addDetector(DetectorType.OPAM, detector -> {
            detector.entryPoint(OpamBuildDetectable.class)
                    .search().defaults();
            detector.entryPoint(OpamLockFileDetectable.class)
                    .search().defaults();
        }).allEntryPointsFallbackToNext();

        return rules.build();
    }
}