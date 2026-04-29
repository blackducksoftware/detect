package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection;

import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Interface for detecting shaded dependencies within a JAR file.
 *
 * Each implementation represents a different detection strategy:
 *   - Method 1: Delta analysis comparing original POM with dependency-reduced-pom.xml
 *   - Method 2: Recursive metadata extraction from embedded pom.properties files
 *   - Method 3: OSGi manifest analysis (Import-Package, Export-Package headers)
 *   - Method 4: SPI descriptor analysis (META-INF/services/ files)
 *   - Method 5: Build info forensics (.buildinfo, build-info.properties)
 *
 * <h2>Exception Contract</h2>
 * <p>
 * <strong>IMPORTANT:</strong> Implementations MUST NOT throw any exceptions.
 * All exceptions must be caught internally and handled gracefully.
 * If an error occurs during detection, implementations should:
 * </p>
 * <ul>
 *   <li>Log the error with appropriate context (file name, entry name, etc.)</li>
 *   <li>Return an empty list or a partial list of successfully discovered dependencies</li>
 *   <li>Never propagate exceptions to the caller</li>
 * </ul>
 * <p>
 * This contract ensures that one failing detector does not break the entire detection pipeline.
 * The Application class runs multiple detectors sequentially, and a failure in one detector
 * should not prevent other detectors from running.
 * </p>
 *
 * <h2>Return Value Contract</h2>
 * <ul>
 *   <li>Never return null - always return an empty list if no dependencies are found</li>
 *   <li>Each DiscoveredDependency should have a valid identifier and detection source</li>
 *   <li>Duplicate entries within a single detector's results are acceptable (deduplication happens at the Application level)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations should be stateless and thread-safe. The same detector instance
 * may be used to analyze multiple JAR files concurrently.
 * </p>
 */
public interface ShadedDependencyInspector {

    /**
     * Detects shaded dependencies within the provided JAR file.
     *
     * <p>
     * This method analyzes the JAR file using the detector's specific strategy
     * and returns a list of discovered shaded dependencies. The implementation
     * must handle all exceptions internally and never throw.
     * </p>
     *
     * @param jarFile The JAR file to analyze for shaded dependencies.
     *                Must not be null. The JAR file should already be opened
     *                and valid (caller's responsibility).
     *
     * @return A list of discovered dependencies, each with its identifier and discovery method.
     *         <ul>
     *           <li>Returns an empty list if no shaded dependencies are found</li>
     *           <li>Returns an empty list if an error occurs during detection</li>
     *           <li>Never returns null</li>
     *         </ul>
     *
     * @implNote Implementations MUST:
     *           <ul>
     *             <li>Catch all exceptions (IOException, parsing errors, etc.)</li>
     *             <li>Log errors with sufficient context for debugging</li>
     *             <li>Return an empty list on failure, not null</li>
     *             <li>Not close the JarFile (caller manages the lifecycle)</li>
     *           </ul>
     */
    List<DiscoveredDependency> detectShadedDependencies(JarFile jarFile);
}
