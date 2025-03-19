package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.representer.Representer;

import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYamlBase;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYamlv5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYaml;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

/**
 * This class does initial parsing and determines if we are dealing with a v5 lock file or something newer.
 */
public class PnpmLockYamlParserInitial {

    private final EnumListFilter<PnpmDependencyType> dependencyFilter;
    private final List<String> excludedDirectories;
    private final List<String> includedDirectories;

    public PnpmLockYamlParserInitial(PnpmLockOptions pnpmLockOptions) {
        this.dependencyFilter = pnpmLockOptions.getDependencyTypeFilter();
        this.excludedDirectories = pnpmLockOptions.getExcludedDirectories();
        this.includedDirectories = pnpmLockOptions.getIncludedDirectories();
    }

    public List<CodeLocation> parse(File pnpmLockYamlFile, @Nullable NameVersion projectNameVersion, PnpmLinkedPackageResolver linkedPackageResolver)
        throws IOException, IntegrationException {
        PnpmLockYamlBase pnpmLockYaml = parseYamlFile(pnpmLockYamlFile);
        
        if (pnpmLockYaml instanceof PnpmLockYaml) {
            PnpmYamlTransformer pnpmYamlTransformer = new PnpmYamlTransformer(dependencyFilter, pnpmLockYaml.lockfileVersion);
            PnpmLockYamlParser pnpmYamlParser = new PnpmLockYamlParser(pnpmYamlTransformer);
            return pnpmYamlParser.parse(pnpmLockYamlFile.getParentFile(), (PnpmLockYaml) pnpmLockYaml, linkedPackageResolver, projectNameVersion, excludedDirectories, includedDirectories);
        } else {
            PnpmYamlTransformerv5 pnpmYamlTransformer = new PnpmYamlTransformerv5(dependencyFilter);
            PnpmLockYamlParserv5 pnpmYamlParser = new PnpmLockYamlParserv5(pnpmYamlTransformer);
            return pnpmYamlParser.parse(pnpmLockYamlFile.getParentFile(), (PnpmLockYamlv5) pnpmLockYaml, linkedPackageResolver, projectNameVersion);
        }
    }

    /**
     * This method reads the pnpm-lock.yaml. It first tries to read it in the current format
     * and then tries v5 if that fails. This is usually faster than first cracking
     * open the yaml file, checking what version it is, and then calling the
     * appropriate reader.
     * 
     * @param pnpmLockYamlFile the File path to the pnpm-lock.yaml file
     * @return a memory representation of the lock file.
     * @throws FileNotFoundException
     */
    private PnpmLockYamlBase parseYamlFile(File pnpmLockYamlFile) throws FileNotFoundException {
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        LoaderOptions loaderOptions = new LoaderOptions();
        try {
            // Try to read the lockfile into the current Yaml classes. It's more common and 
            // should hopefully work more of the time.
            Yaml yaml = new Yaml(new Constructor(PnpmLockYaml.class, loaderOptions), representer);
            return yaml.load(new FileReader(pnpmLockYamlFile));
        } catch (ConstructorException e) {
            // If the reading fails try to read a v5 Yaml. 
            Yaml yaml = new Yaml(new Constructor(PnpmLockYamlv5.class, loaderOptions), representer);
            return yaml.load(new FileReader(pnpmLockYamlFile));
        }
    }
}
