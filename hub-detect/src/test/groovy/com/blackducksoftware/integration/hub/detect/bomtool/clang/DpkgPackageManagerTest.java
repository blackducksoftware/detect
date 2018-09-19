package com.blackducksoftware.integration.hub.detect.bomtool.clang;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableOutput;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunner;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunnerException;

public class DpkgPackageManagerTest {

    @Test
    public void test() throws ExecutableRunnerException {
        StringBuilder sb = new StringBuilder();
        sb.append("garbage\n");
        sb.append("nonsense\n");
        sb.append("libc6-dev:amd64: /usr/include/stdlib.h\n");
        final String pkgMgrOwnedByOutput = sb.toString();

        sb = new StringBuilder();
        sb.append("Package: libc6-dev\n");
        sb.append("Status: install ok installed\n");
        sb.append("Priority: optional\n");
        sb.append("Section: libdevel\n");
        sb.append("Installed-Size: 18812\n");
        sb.append("Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>\n");
        sb.append("Architecture: amd64\n");
        sb.append("Multi-Arch: same\n");
        sb.append("Source: glibc\n");
        sb.append("Version: 2.27-3ubuntu1\n");
        sb.append("Provides: libc-dev\n");
        sb.append("Depends: libc6 (= 2.27-3ubuntu1), libc-dev-bin (= 2.27-3ubuntu1), linux-libc-dev\n");
        sb.append("Suggests: glibc-doc, manpages-dev\n");
        sb.append(
                "Breaks: binutils (<< 2.26), binutils-gold (<< 2.20.1-11), cmake (<< 2.8.4+dfsg.1-5), gcc-4.4 (<< 4.4.6-4), gcc-4.5 (<< 4.5.3-2), gcc-4.6 (<< 4.6.0-12), libhwloc-dev (<< 1.2-3), libjna-java (<< 3.2.7-4), liblouis-dev (<< 2.3.0-2), liblouisxml-dev (<< 2.4.0-2), libperl5.26 (<< 5.26.1-3), make (<< 3.81-8.1), pkg-config (<< 0.26-1)\n");
        sb.append("Conflicts: libc0.1-dev, libc0.3-dev, libc6.1-dev\n");
        sb.append("Description: GNU C Library: Development Libraries and Header Files\n");
        sb.append(" Contains the symlinks, headers, and object files needed to compile\n");
        sb.append(" and link programs which use the standard C library.\n");
        sb.append("Homepage: https://www.gnu.org/software/libc/libc.html\n");
        sb.append("Original-Maintainer: GNU Libc Maintainers <debian-glibc@lists.debian.org>\n");

        final String pkgMgrPkgInfoOutput = sb.toString();

        final DpkgPackageManager pkgMgr = new DpkgPackageManager();
        final ExecutableRunner executableRunner = Mockito.mock(ExecutableRunner.class);
        Mockito.when(executableRunner.executeQuietly("dpkg", Arrays.asList("-S", "/usr/include/stdlib.h"))).thenReturn(new ExecutableOutput(0, pkgMgrOwnedByOutput, ""));
        Mockito.when(executableRunner.executeQuietly("dpkg", "-s", "libc6-dev")).thenReturn(new ExecutableOutput(0, pkgMgrPkgInfoOutput, ""));

        final DependencyFileDetails dependencyFile = new DependencyFileDetails(false, new File("/usr/include/stdlib.h"));
        final List<PackageDetails> pkgs = pkgMgr.getPackages(executableRunner, new HashSet<>(), dependencyFile);
        assertEquals(1, pkgs.size());
        assertEquals("libc6-dev", pkgs.get(0).getPackageName());
        assertEquals("2.27-3ubuntu1", pkgs.get(0).getPackageVersion());
        assertEquals("amd64", pkgs.get(0).getPackageArch());
    }

}
