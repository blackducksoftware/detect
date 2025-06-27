package com.blackduck.integration.detect.workflow.blackduck.developer.aggregate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.blackduck.integration.blackduck.api.generated.component.*;
import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;

import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;

public class RapidScanResultAggregator {
    
    private final Map<String, Set<String>> directToTransitiveChildren = new HashMap<>();
    private final Map<String, String[]> directUpgradeGuidanceVersions = new HashMap<>();
    
    public RapidScanAggregateResult aggregateData(List<DeveloperScansScanView> results, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        Collection<RapidScanComponentDetail> componentDetails = aggregateComponentData(results, severitiesToFailPolicyCheck);
        List<RapidScanComponentDetail> sortedByComponent = componentDetails.stream()
                .sorted(Comparator.comparing(RapidScanComponentDetail::getComponentIdentifier))
                .collect(Collectors.toList());
        Map<RapidScanDetailGroup, RapidScanComponentGroupDetail> aggregatedDetails = new HashMap<>();
        aggregatedDetails.put(RapidScanDetailGroup.POLICY, new RapidScanComponentGroupDetail(RapidScanDetailGroup.POLICY));
        aggregatedDetails.put(RapidScanDetailGroup.SECURITY, new RapidScanComponentGroupDetail(RapidScanDetailGroup.SECURITY));
        aggregatedDetails.put(RapidScanDetailGroup.LICENSE, new RapidScanComponentGroupDetail(RapidScanDetailGroup.LICENSE));
        aggregatedDetails.put(RapidScanDetailGroup.VIOLATING_POLICIES, new RapidScanComponentGroupDetail(RapidScanDetailGroup.VIOLATING_POLICIES));

        RapidScanResultSummary.Builder summaryBuilder = new RapidScanResultSummary.Builder();

        for (RapidScanComponentDetail detail : sortedByComponent) {
            summaryBuilder.addDetailData(detail);
            
            RapidScanComponentGroupDetail aggregatedSecurityDetail = aggregatedDetails.get(detail.getSecurityDetails().getGroup());
            RapidScanComponentGroupDetail aggregatedLicenseDetail = aggregatedDetails.get(detail.getLicenseDetails().getGroup());
            RapidScanComponentGroupDetail aggregatedComponentDetail = aggregatedDetails.get(detail.getComponentDetails().getGroup());
            RapidScanComponentGroupDetail aggregatedViolatingPoliciesDetail = aggregatedDetails.get(detail.getViolatingPoliciesDetails().getGroup());

            aggregatedComponentDetail.addErrors(detail.getComponentDetails().getErrorMessages());
            aggregatedComponentDetail.addWarnings(detail.getComponentDetails().getWarningMessages());
            aggregatedSecurityDetail.addErrors(detail.getSecurityDetails().getErrorMessages());
            aggregatedSecurityDetail.addWarnings(detail.getSecurityDetails().getWarningMessages());
            aggregatedLicenseDetail.addErrors(detail.getLicenseDetails().getErrorMessages());
            aggregatedLicenseDetail.addWarnings(detail.getLicenseDetails().getWarningMessages());
            aggregatedViolatingPoliciesDetail.addErrors(detail.getViolatingPoliciesDetails().getErrorMessages());
            aggregatedViolatingPoliciesDetail.addWarnings(detail.getViolatingPoliciesDetails().getWarningMessages());
        }

        List<String> transitiveGuidance = this.transitiveGuidanceDetails();
        summaryBuilder.addTransitiveGuidances(new LinkedHashSet<>(transitiveGuidance));
        return new RapidScanAggregateResult(summaryBuilder.build(), aggregatedDetails.get(RapidScanDetailGroup.POLICY),
                aggregatedDetails.get(RapidScanDetailGroup.SECURITY),
                aggregatedDetails.get(RapidScanDetailGroup.LICENSE),
                aggregatedDetails.get(RapidScanDetailGroup.VIOLATING_POLICIES),
                transitiveGuidance);
    }

    private List<RapidScanComponentDetail> aggregateComponentData(List<DeveloperScansScanView> results, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        // the key is the component identifier
        List<RapidScanComponentDetail> componentDetails = new LinkedList<>();

        for (DeveloperScansScanView resultView : results) {
            this.compileTransitiveGuidance(resultView);

            RapidScanComponentDetail componentDetail = createDetail(resultView);
            componentDetails.add(componentDetail);
            RapidScanComponentGroupDetail componentGroupDetail = componentDetail.getComponentDetails();
            RapidScanComponentGroupDetail securityGroupDetail = componentDetail.getSecurityDetails();
            RapidScanComponentGroupDetail licenseGroupDetail = componentDetail.getLicenseDetails();
            RapidScanComponentGroupDetail violatingPoliciesDetail = componentDetail.getViolatingPoliciesDetails();

            List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolations =
                    resultView.getComponentViolatingPolicies();
            List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> vulnerabilityViolations = resultView
                    .getPolicyViolationVulnerabilities();
            List<DeveloperScansScanItemsPolicyViolationLicensesView> licenseViolations = resultView
                    .getPolicyViolationLicenses();
            List<DeveloperScansScanItemsViolatingPoliciesView> policyViolationsSuperset =
                    resultView.getViolatingPolicies();

            Set<String> allViolatedPolicyNames = policyViolationsSuperset.stream()
                    .map(DeveloperScansScanItemsViolatingPoliciesView::getPolicyName)
                    .collect(Collectors.toSet());

           Set<String> componentPolicyNames = componentViolations.stream()
                    .map(DeveloperScansScanItemsComponentViolatingPoliciesView::getPolicyName)
                    .collect(Collectors.toSet());

            Set<String> vulnerabilityPolicyNames = vulnerabilityViolations.stream()
                    .map(DeveloperScansScanItemsPolicyViolationVulnerabilitiesView::getViolatingPolicies)
                    .flatMap(Collection::stream)
                    .map(DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView::getPolicyName)
                    .collect(Collectors.toSet());

            Set<String> licensePolicyNames = licenseViolations.stream()
                    .map(DeveloperScansScanItemsPolicyViolationLicensesView::getViolatingPolicies)
                    .flatMap(Collection::stream)
                    .map(DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView::getPolicyName)
                    .collect(Collectors.toSet());

            componentGroupDetail.addPolicies(componentPolicyNames);
            securityGroupDetail.addPolicies(vulnerabilityPolicyNames);
            licenseGroupDetail.addPolicies(licensePolicyNames);
            violatingPoliciesDetail.addPolicies(allViolatedPolicyNames);

            addComponentData(resultView, componentViolations, componentGroupDetail, severitiesToFailPolicyCheck);
            addVulnerabilityData(resultView, vulnerabilityViolations, securityGroupDetail, severitiesToFailPolicyCheck);
            addLicenseData(resultView, licenseViolations, licenseGroupDetail, severitiesToFailPolicyCheck);
            addViolatingPoliciesData(resultView, policyViolationsSuperset, violatingPoliciesDetail, severitiesToFailPolicyCheck);
        }
        
        return componentDetails;
    }

    /**
     * This is handled here because we have to use the data in a "look back" type of scenario.  
     * We are getting lists of components and they in turn have transitive guidance if they are 
     * transitive components.  We must base this list on the direct dependency component that come
     * in the trans. guidance section, effectively reversing the order.  By handling that here, we
     * can get a proper lookup of direct comp. -> child component(s).  Once this is completed,
     * we can put together the string messages needed.
     * 
     * @param resultView 
     * @param results
     */
    private void compileTransitiveGuidance(DeveloperScansScanView resultView) {
        List<DeveloperScansScanItemsTransitiveUpgradeGuidanceView> transitiveGuidance = resultView.getTransitiveUpgradeGuidance();
        if (transitiveGuidance != null && !transitiveGuidance.isEmpty()) {
            String childExternalId = resultView.getExternalId();
            for (DeveloperScansScanItemsTransitiveUpgradeGuidanceView guidance : transitiveGuidance) {
                String parentId = guidance.getExternalId();
                String[] versions = getversionsFromUpgradeGuidance(guidance);
                this.directUpgradeGuidanceVersions.put(parentId, versions);
                addChildrenToParent(parentId, childExternalId);
            }
        }
    }
    
    private void addChildrenToParent(String parentId, String childExternalId) {
        // accumulate transitive information for direct dep. and list of child deps.
        if (directToTransitiveChildren.containsKey(parentId)) {
            directToTransitiveChildren.get(parentId).add(childExternalId);
        } else {
            Set<String> children = new HashSet<>();
            children.add(childExternalId);
            directToTransitiveChildren.put(parentId, children);
        }
    }

    private String[] getversionsFromUpgradeGuidance(DeveloperScansScanItemsTransitiveUpgradeGuidanceView guidance) {
        DeveloperScansScanItemsTransitiveUpgradeGuidanceShortTermUpgradeGuidanceView stg = guidance.getShortTermUpgradeGuidance();
        DeveloperScansScanItemsTransitiveUpgradeGuidanceLongTermUpgradeGuidanceView ltg = guidance.getLongTermUpgradeGuidance();
        String shortTermVersion = stg != null ? stg.getVersionName() : "";
        String longTermVersion = ltg != null ? ltg.getVersionName() : "";
        /* stg or ltg can be null.  If one or the other is null, then we'll 
        overwrite the one of the versions with the non-null version
        and suggest that in the guidance (as if stg.version = ltg.version) */
        if ( stg == null && ltg != null) {
            shortTermVersion = longTermVersion;
        } else if (stg != null && ltg == null) {
            longTermVersion = shortTermVersion;
        }
        return new String[] { shortTermVersion, longTermVersion };
    }  

    private RapidScanComponentDetail createDetail(DeveloperScansScanView resultView) {
        String componentName = resultView.getComponentName();
        String componentVersion = resultView.getVersionName();
        String componentIdentifier = "";
        
        if (StringUtils.isNotBlank(resultView.getComponentIdentifier())) {
            componentIdentifier = resultView.getComponentIdentifier();
        } else if (StringUtils.isNotBlank(resultView.getExternalId())) {
            componentIdentifier = resultView.getExternalId();
        }
        
        RapidScanComponentGroupDetail componentGroupDetail = new RapidScanComponentGroupDetail(RapidScanDetailGroup.POLICY);
        RapidScanComponentGroupDetail securityGroupDetail = new RapidScanComponentGroupDetail(RapidScanDetailGroup.SECURITY);
        RapidScanComponentGroupDetail licenseGroupDetail = new RapidScanComponentGroupDetail(RapidScanDetailGroup.LICENSE);
        RapidScanComponentGroupDetail violatingPoliciesDetail = new RapidScanComponentGroupDetail(RapidScanDetailGroup.VIOLATING_POLICIES);

        return new RapidScanComponentDetail(componentName, componentVersion, componentIdentifier, componentGroupDetail,
                securityGroupDetail, licenseGroupDetail, violatingPoliciesDetail);
    }

    private void addVulnerabilityData(DeveloperScansScanView resultView, List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> vulnerabilities, RapidScanComponentGroupDetail securityDetail, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        for (DeveloperScansScanItemsPolicyViolationVulnerabilitiesView vulnerabilityPolicyViolation : vulnerabilities) {
            securityDetail.addVulnerabilityMessages(resultView, vulnerabilityPolicyViolation, severitiesToFailPolicyCheck);
        }
    }

    private void addLicenseData(DeveloperScansScanView resultView, List<DeveloperScansScanItemsPolicyViolationLicensesView> licenseViolations, RapidScanComponentGroupDetail licenseDetail, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        for (DeveloperScansScanItemsPolicyViolationLicensesView licensePolicyViolation : licenseViolations) {
            licenseDetail.addLicenseMessages(resultView, licensePolicyViolation, severitiesToFailPolicyCheck);
        }
    }
    
    private void addComponentData(DeveloperScansScanView resultView, List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolations, RapidScanComponentGroupDetail componentGroupDetail, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        for (DeveloperScansScanItemsComponentViolatingPoliciesView componentPolicyViolation: componentViolations) {
            componentGroupDetail.addComponentMessages(resultView, componentPolicyViolation, severitiesToFailPolicyCheck);
        }
    }

    private void addViolatingPoliciesData(DeveloperScansScanView resultView, List<DeveloperScansScanItemsViolatingPoliciesView> allPolicyViolations, RapidScanComponentGroupDetail violatingPoliciesDetail, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        violatingPoliciesDetail.addViolatingPoliciesMessages(resultView, allPolicyViolations, severitiesToFailPolicyCheck);
    }

    /**
     * Special case handler for formatting the transitive guidance suggestions and handing
     * them off to be reported...
     * 
     * @return  String list of transitive guidance suggestions.
     */
    private List<String> transitiveGuidanceDetails() {
        ArrayList<String> guidances = new ArrayList<>();

        Set<String> componentKeys = this.directToTransitiveChildren.keySet();
        for (String key : componentKeys) {
            String plural = "s";
            String externalId = key;

            Set<String> children = this.directToTransitiveChildren.get(externalId);
            String childComponents = StringUtils.join(children, ", ");
            if (children.size() == 1) {
                plural = "";
            }
            
            String versionsToUse;
            String[] versions = this.directUpgradeGuidanceVersions.get(externalId);
            if (versions[1] != null && versions[0] != null   && versions[1].equals(versions[0])) {
                versionsToUse = "version ".concat(versions[1]);
            } else {
                versionsToUse = "versions (Short Term) ".concat(versions[0]).concat(", ").concat("(Long Term) ").concat(versions[1]);
            }
            
            String guidance = String.format("Upgrade component %s to %s in order to upgrade transitive component%s %s", externalId, versionsToUse, plural, childComponents);
            guidances.add(guidance);
        }
        return guidances;
    }
}
