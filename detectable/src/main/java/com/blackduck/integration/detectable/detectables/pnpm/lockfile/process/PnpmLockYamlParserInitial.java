package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final EnumListFilter<PnpmDependencyType> dependencyFilter;
    private final List<String> excludedDirectories;
    private final List<String> includedDirectories;

    public PnpmLockYamlParserInitial(PnpmLockOptions pnpmLockOptions) {
        this.dependencyFilter = pnpmLockOptions.getDependencyTypeFilter();
        this.excludedDirectories = pnpmLockOptions.getExcludedDirectories();
        this.includedDirectories = pnpmLockOptions.getIncludedDirectories();
    }

    /**
     * Parses a pnpm-lock.yaml file and returns the extracted code locations.
     *
     * <p>Conditional paths:
     * <ol>
     *   <li>If the YAML file is empty or contains no content, SnakeYAML returns null
     *       → log a warning and return an empty list (empty BOM).</li>
     *   <li>If parsed as {@link PnpmLockYaml} (v6/v9 format):
     *       <ul>
     *         <li>If {@code lockfileVersion} is null → throw {@link IntegrationException}
     *             (the file is malformed and cannot be processed).</li>
     *         <li>Otherwise → delegate to {@link PnpmLockYamlParser} for v6/v9 processing.</li>
     *       </ul>
     *   </li>
     *   <li>If parsed as {@link PnpmLockYamlv5} (v5 format) → delegate to
     *       {@link PnpmLockYamlParserv5} for v5 processing.</li>
     * </ol>
     */
    public List<CodeLocation> parse(File pnpmLockYamlFile, @Nullable NameVersion projectNameVersion, PnpmLinkedPackageResolver linkedPackageResolver)
        throws IOException, IntegrationException {
        PnpmLockYamlBase pnpmLockYaml = parseYamlFile(pnpmLockYamlFile);

        // Path 1: SnakeYAML returned null — the file is empty or contains only comments.
        if (pnpmLockYaml == null) {
            logger.warn("The pnpm-lock.yaml file '{}' is empty and contains no parsable content. No dependencies will be extracted.",
                pnpmLockYamlFile.getAbsolutePath());
            return Collections.emptyList();
        }

        // Path 2: Parsed as v6/v9 format (PnpmLockYaml).
        if (pnpmLockYaml instanceof PnpmLockYaml) {
            if (pnpmLockYaml.lockfileVersion == null) {
                // Path 2a: v6/v9 model but lockfileVersion is missing — malformed file.
                logger.debug("Parsed as PnpmLockYaml (v6/v9 model) but lockfileVersion is null. The file appears malformed.");
                throw new IntegrationException(
                    "The pnpm-lock.yaml file does not contain a 'lockfileVersion' field. "
                    + "This is required for parsing. Please regenerate the lock file by running 'pnpm install'.");
            }
            // Path 2b: Valid v6/v9 lockfile — process normally.
            logger.debug("Detected v6/v9 lockfile (version: {}). Proceeding with v6/v9 parser.", pnpmLockYaml.lockfileVersion);
            PnpmYamlTransformer pnpmYamlTransformer = new PnpmYamlTransformer(dependencyFilter, pnpmLockYaml.lockfileVersion);
            PnpmLockYamlParser pnpmYamlParser = new PnpmLockYamlParser(pnpmYamlTransformer);
            return pnpmYamlParser.parse(pnpmLockYamlFile.getParentFile(), (PnpmLockYaml) pnpmLockYaml, linkedPackageResolver, projectNameVersion, excludedDirectories, includedDirectories);
        } else {
            // Path 3: Parsed as v5 format (PnpmLockYamlv5).
            logger.debug("Detected v5 lockfile (version: {}). Proceeding with v5 parser.", pnpmLockYaml.lockfileVersion);
            PnpmYamlTransformerv5 pnpmYamlTransformer = new PnpmYamlTransformerv5(dependencyFilter);
            PnpmLockYamlParserv5 pnpmYamlParser = new PnpmLockYamlParserv5(pnpmYamlTransformer);
            return pnpmYamlParser.parse(pnpmLockYamlFile.getParentFile(), (PnpmLockYamlv5) pnpmLockYaml, linkedPackageResolver, projectNameVersion);
        }
    }

    /**
     * Reads and deserialises a pnpm-lock.yaml file into a model object.
     *
     * <p>Strategy — try v6/v9 first, fall back to v5:
     * <ol>
     *   <li>Attempt to parse as {@link PnpmLockYaml} (v6/v9). Three outcomes:
     *       <ul>
     *         <li><b>Success with v6+ version</b> → return the result immediately.</li>
     *         <li><b>Success but version indicates v5</b> (or is null) → fall through
     *             to re-parse as v5. This happens because {@code setSkipMissingProperties(true)}
     *             allows SnakeYAML to silently ignore v5-specific fields.</li>
     *         <li><b>ConstructorException</b> → log and fall through to v5 parsing.</li>
     *       </ul>
     *   </li>
     *   <li>Parse as {@link PnpmLockYamlv5}. The result may be null if the file is empty.</li>
     * </ol>
     *
     * @param pnpmLockYamlFile the File path to the pnpm-lock.yaml file
     * @return a memory representation of the lock file, or null if the file is empty
     * @throws IOException if the file cannot be read
     */
    private PnpmLockYamlBase parseYamlFile(File pnpmLockYamlFile) throws IOException {
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        LoaderOptions loaderOptions = new LoaderOptions();

        try {
            // Step 1: Try to read the lockfile into the v6/v9 Yaml classes first (more common).
            logger.debug("Attempting to parse '{}' as v6/v9 format.", pnpmLockYamlFile.getName());
            Yaml yaml = new Yaml(new Constructor(PnpmLockYaml.class, loaderOptions), representer);
            PnpmLockYamlBase result;
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(pnpmLockYamlFile), StandardCharsets.UTF_8)) {
                result = yaml.load(reader);
            }

            if (result == null) {
                // Step 1a: File was empty or contained only comments — SnakeYAML returns null.
                logger.debug("SnakeYAML returned null for '{}'. The file appears to be empty.", pnpmLockYamlFile.getName());
                return null;
            }

            if (isV6OrNewer(result.lockfileVersion)) {
                // Step 1b: Valid v6+ lockfile confirmed by version check.
                logger.debug("Successfully parsed as v6/v9 format (lockfileVersion: {}).", result.lockfileVersion);
                return result;
            }

            // Step 1c: Parsed successfully but version indicates v5 or older.
            // setSkipMissingProperties(true) allowed the parse to succeed even though v5 has
            // different field structures. Must re-parse with the correct v5 model class.
            logger.debug("Parsed successfully but lockfileVersion '{}' indicates v5 or older. Will re-parse with v5 model.",
                result.lockfileVersion);
        } catch (ConstructorException e) {
            // Step 1d: SnakeYAML could not map the YAML structure to PnpmLockYaml.
            // This can happen when v5-specific fields conflict with the v6/v9 model.
            logger.debug("Failed to parse '{}' as v6/v9 format, falling back to v5 parsing.", pnpmLockYamlFile.getName(), e);
        }

        // Step 2: Re-parse as v5.
        logger.debug("Attempting to parse '{}' as v5 format.", pnpmLockYamlFile.getName());
        Yaml yaml = new Yaml(new Constructor(PnpmLockYamlv5.class, loaderOptions), representer);
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(pnpmLockYamlFile), StandardCharsets.UTF_8)) {
            return yaml.load(reader);
        }
    }

    /**
     * Returns true if the lockfileVersion string represents a v6 or newer pnpm lockfile.
     * Returns false if the version is v5 or older, or null.
     */
    private boolean isV6OrNewer(@Nullable String lockfileVersion) {
        if (lockfileVersion == null) {
            return false;
        }
        try {
            return Double.parseDouble(lockfileVersion) >= 6.0;
        } catch (NumberFormatException e) {
            logger.debug("Unable to parse lockfileVersion '{}'; treating as v5/unknown.", lockfileVersion);
            return false;
        }
    }
}
