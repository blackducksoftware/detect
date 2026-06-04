package com.blackduck.integration.detect.battery.docker.integration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.blackduck.api.generated.view.CodeLocationView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionComponentVersionView;
import com.blackduck.integration.blackduck.api.manual.view.ProjectView;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.dataservice.ProjectBomService;
import com.blackduck.integration.blackduck.service.dataservice.ProjectService;
import com.blackduck.integration.blackduck.service.model.ProjectSyncModel;
import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.common.util.Bds;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BlackDuckAssertions {
    private final ProjectService projectService;
    private final BlackDuckApiClient blackDuckApiClient;
    private final ProjectBomService projectBomService;
    private final NameVersion projectNameVersion;

    private Set<String> codeLocationNames;
    private ProjectVersionWrapper projectVersionWrapper;
    private List<ProjectVersionComponentVersionView> bomComponents;

    public BlackDuckAssertions(BlackDuckServicesFactory blackDuckServicesFactory, NameVersion projectNameVersion) {
        projectService = blackDuckServicesFactory.createProjectService();
        blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
        projectBomService = blackDuckServicesFactory.createProjectBomService();
        this.projectNameVersion = projectNameVersion;
    }

    public ProjectVersionWrapper emptyOnBlackDuck() throws IntegrationException {
        Optional<ProjectVersionWrapper> optionalProjectVersionWrapper = projectService.getProjectVersion(projectNameVersion);
        if (optionalProjectVersionWrapper.isPresent()) {
            blackDuckApiClient.delete(optionalProjectVersionWrapper.get().getProjectView());
        }

        ProjectSyncModel projectSyncModel = ProjectSyncModel.createWithDefaults(projectNameVersion);
        projectService.syncProjectAndVersion(projectSyncModel);

        optionalProjectVersionWrapper = projectService.getProjectVersion(projectNameVersion);
        assertTrue(optionalProjectVersionWrapper.isPresent());

        List<CodeLocationView> codeLocations = blackDuckApiClient.getAllResponses(optionalProjectVersionWrapper.get().getProjectVersionView().metaCodelocationsLink());
        assertEquals(0, codeLocations.size());

        List<ProjectVersionComponentVersionView> bomComponents = projectBomService.getComponentsForProjectVersion(optionalProjectVersionWrapper.get().getProjectVersionView());
        assertEquals(0, bomComponents.size());

        return optionalProjectVersionWrapper.get();
    }

    public void deleteProjectAndCodeLocations() throws IntegrationException {
        Optional<ProjectVersionWrapper> optionalProjectVersionWrapper = projectService.getProjectVersion(projectNameVersion);
        if (!optionalProjectVersionWrapper.isPresent()) {
            return;
        }
        ProjectVersionWrapper projectVersionWrapper = optionalProjectVersionWrapper.get();
        List<CodeLocationView> codeLocationsToDelete = blackDuckApiClient.getAllResponses(projectVersionWrapper.getProjectVersionView().metaCodelocationsLink());
        for (CodeLocationView toDelete : codeLocationsToDelete) {
            blackDuckApiClient.delete(toDelete);
        }

        blackDuckApiClient.delete(projectVersionWrapper.getProjectView());
    }

    public ProjectVersionWrapper getProjectVersionWrapper() throws IntegrationException {
        if (projectVersionWrapper == null) {
            projectVersionWrapper = projectService.getProjectVersion(projectNameVersion).orElseThrow(() -> new RuntimeException("Project Version expected but could not be found on Black Duck."));
        }
        return projectVersionWrapper;
    }

    public void assertExactCodeLocations(Set<String> codeLocationNames) throws IntegrationException {
        SetAssertionUtil.assertSetDifferences(getCodeLocationNames(), codeLocationNames,
            expectedMissing -> Assertions.fail(String.format("Expected code location %s but could not find it!", expectedMissing)),
            extraActual -> Assertions.fail(String.format("An additional code location %s was found but was not expected!", extraActual))
        );
    }

    public void hasCodeLocations(String... codeLocationNames) throws IntegrationException {
        hasCodeLocations(Bds.of(codeLocationNames).toSet());
    }

    public void hasCodeLocations(Set<String> codeLocationNames) throws IntegrationException {
        SetAssertionUtil.assertSetDifferences(getCodeLocationNames(), codeLocationNames,
            expectedMissing -> Assertions.fail(String.format("Expected code location %s but could not find it!", expectedMissing)),
            extraActual -> {/* no-op, extra code locations fine, only checking it HAS the given. */}
        );
    }

    public void hasComponents(String... componentNames) throws IntegrationException {
        hasComponents(Bds.of(componentNames).toSet());
    }

    public void hasComponents(Set<String> componentNames) throws IntegrationException {
        List<ProjectVersionComponentVersionView> bomComponents = getBomComponents();


        componentNames.forEach(componentName -> {
            Optional<ProjectVersionComponentVersionView> blackDuckCommonComponent = bomComponents.stream()
                .filter(ProjectVersionComponentView -> componentName.equals(ProjectVersionComponentView.getComponentName()))
                .findFirst();
            // Message supplier is only evaluated on failure, avoiding the cost of
            // building the full component name string on every successful assertion.
            assertTrue(
                blackDuckCommonComponent.isPresent(),
                () -> {
                    // Surfaces full BOM contents in assertion failure message for easier debugging
                    String allComponentNames = bomComponents.stream()
                        .map(ProjectVersionComponentVersionView::getComponentName)
                        .collect(Collectors.joining("', '", "['", "']"));
                    return String.format(
                        "Component '%s' NOT FOUND in BOM!%nBOM contains %d components: %s",
                        componentName,
                        bomComponents.size(),
                        allComponentNames
                    );
                }
            );
        });
    }

    public void doesNotHaveComponents(String... componentNames) throws IntegrationException {
        doesNotHaveComponents(Bds.of(componentNames).toSet());
    }

    public void doesNotHaveComponents(Set<String> componentNames) throws IntegrationException {
        List<ProjectVersionComponentVersionView> bomComponents = getBomComponents();
        componentNames.forEach(componentName -> {
            Optional<ProjectVersionComponentVersionView> blackDuckCommonComponent = bomComponents.stream()
                    .filter(ProjectVersionComponentView -> componentName.equals(ProjectVersionComponentView.getComponentName()))
                    .findFirst();
            assertFalse(blackDuckCommonComponent.isPresent());
        });
    }

    public void checkComponentVersionExists(String componentName, String version) throws IntegrationException {
        List<ProjectVersionComponentVersionView> bomComponents = getBomComponents();
        Optional<ProjectVersionComponentVersionView> blackDuckCommonComponent = bomComponents.stream()
                .filter(ProjectVersionComponentView -> componentName.equals(ProjectVersionComponentView.getComponentName()))
                .findFirst();
        assertTrue(blackDuckCommonComponent.isPresent());
        String blackDuckCommonComponentVersion = blackDuckCommonComponent.get().getComponentVersionName();
        assertEquals(version, blackDuckCommonComponentVersion);
    }

    public void checkComponentVersionNotExists(String componentName, String version) throws IntegrationException {
        List<ProjectVersionComponentVersionView> bomComponents = getBomComponents();
        Optional<ProjectVersionComponentVersionView> blackDuckCommonComponent = bomComponents.stream()
                .filter(ProjectVersionComponentView -> componentName.equals(ProjectVersionComponentView.getComponentName()))
                .findFirst();
        assertTrue(blackDuckCommonComponent.isPresent());
        String blackDuckCommonComponentVersion = blackDuckCommonComponent.get().getComponentVersionName();
        assertNotEquals(version, blackDuckCommonComponentVersion);
    }

    public NameVersion getProjectNameVersion() {
        return projectNameVersion;
    }

    public ProjectView retrieveProjectView() throws IntegrationException {
        return getProjectVersionWrapper().getProjectView();
    }

    public void codeLocationCount(int codeLocationCount) throws IntegrationException {
        Assertions.assertEquals(codeLocationCount, blackDuckApiClient.getAllResponses(getProjectVersionWrapper().getProjectVersionView().metaCodelocationsLink()).size());
    }

    private Set<String> getCodeLocationNames() throws IntegrationException {
        if (codeLocationNames == null) {
            List<CodeLocationView> codeLocations = blackDuckApiClient.getAllResponses(getProjectVersionWrapper().getProjectVersionView().metaCodelocationsLink());
            codeLocationNames = codeLocations.stream().map(CodeLocationView::getName).collect(Collectors.toSet());
        }
        return codeLocationNames;
    }

    private List<ProjectVersionComponentVersionView> getBomComponents() throws IntegrationException {
        if (bomComponents == null) {
            bomComponents = projectBomService.getComponentsForProjectVersion(getProjectVersionWrapper().getProjectVersionView());
        }
        return bomComponents;
    }
}
