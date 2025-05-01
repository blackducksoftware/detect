package com.blackduck.integration.detect.workflow.blackduck.developer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsComponentViolatingPoliciesView;
import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsPolicyViolationLicensesView;
import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView;
import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsPolicyViolationVulnerabilitiesView;
import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView;
import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsViolatingPoliciesView;
import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanAggregateResult;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultAggregator;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultSummary;
import com.blackduck.integration.log.BufferedIntLogger;
import com.blackduck.integration.log.LogLevel;

import static org.junit.jupiter.api.Assertions.*;

public class RapidScanResultAggregatorTest {
    @Test
    public void testEmptyResults() {
        List<DeveloperScansScanView> results = Collections.emptyList();
        RapidScanResultAggregator aggregator = new RapidScanResultAggregator();
        RapidScanAggregateResult aggregateResult = aggregator.aggregateData(results, null);
        BufferedIntLogger logger = new BufferedIntLogger();
        aggregateResult.logResult(logger);
        RapidScanResultSummary summary = aggregateResult.getSummary();
        assertEquals(0, summary.getPolicyErrorCount());
        assertEquals(0, summary.getPolicyWarningCount());
        assertEquals(0, summary.getSecurityErrorCount());
        assertEquals(0, summary.getSecurityWarningCount());
        assertEquals(0, summary.getLicenseErrorCount());
        assertEquals(0, summary.getLicenseWarningCount());
        assertFalse(logger.getOutputList(LogLevel.INFO).isEmpty());
    }

    @Test
    public void testResults() {
        List<DeveloperScansScanView> results = createResultList();
        RapidScanResultAggregator aggregator = new RapidScanResultAggregator();
        List<PolicyRuleSeverityType> violatingPoicies = 
                new ArrayList<>(Arrays.asList(PolicyRuleSeverityType.BLOCKER, PolicyRuleSeverityType.CRITICAL));
        RapidScanAggregateResult aggregateResult = aggregator.aggregateData(results, violatingPoicies);
        BufferedIntLogger logger = new BufferedIntLogger();
        aggregateResult.logResult(logger);
        RapidScanResultSummary summary = aggregateResult.getSummary();
        assertEquals(1, summary.getPolicyErrorCount());
        assertEquals(1, summary.getPolicyWarningCount());
        assertEquals(1, summary.getSecurityErrorCount());
        assertEquals(1, summary.getSecurityWarningCount());
        assertEquals(1, summary.getLicenseErrorCount());
        assertEquals(1, summary.getLicenseWarningCount());
        assertEquals(1, summary.getAllOtherPolicyErrorCount());
        assertEquals(1, summary.getAllOtherPolicyWarningCount());
        assertTrue(summary.getPolicyViolationNames().contains("other_policy"));
        assertEquals(7, summary.getPolicyViolationNames().size());
        assertFalse(logger.getOutputList(LogLevel.INFO).isEmpty());
    }

    private List<DeveloperScansScanView> createResultList() {
        List<DeveloperScansScanView> resultList = new ArrayList<>();
        DeveloperScansScanView view = createView();
        resultList.add(view);
        return resultList;
    }

    private DeveloperScansScanView createView() {
        return new DeveloperScansScanView() {
            @Override
            public String getComponentName() {
                return "component_1";
            }

            @Override
            public String getVersionName() {
                return "component_version_1";
            }

            @Override
            public String getComponentIdentifier() {
                return "component_1:component_version_1";
            }
            
            @Override
            public List<DeveloperScansScanItemsComponentViolatingPoliciesView> getComponentViolatingPolicies() {
                List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolatingPolicies = new ArrayList<>();
                
                DeveloperScansScanItemsComponentViolatingPoliciesView componentViolatingPolicy = new DeveloperScansScanItemsComponentViolatingPoliciesView();
                componentViolatingPolicy.setPolicyName("component_policy");
                componentViolatingPolicy.setPolicySeverity("CRITICAL");
                
                DeveloperScansScanItemsComponentViolatingPoliciesView componentViolatingPolicy2 = new DeveloperScansScanItemsComponentViolatingPoliciesView();
                componentViolatingPolicy2.setPolicyName("component_policy_warning");
                componentViolatingPolicy2.setPolicySeverity("MINOR");
                
                componentViolatingPolicies.add(componentViolatingPolicy);
                componentViolatingPolicies.add(componentViolatingPolicy2);
                return componentViolatingPolicies;
            }

            @Override
            public List<DeveloperScansScanItemsViolatingPoliciesView> getViolatingPolicies() {
                List<DeveloperScansScanItemsViolatingPoliciesView> violatingPolicies = new ArrayList<>();

                DeveloperScansScanItemsViolatingPoliciesView view = new DeveloperScansScanItemsViolatingPoliciesView();
                view.setPolicyName("component_policy");
                view.setPolicySeverity("CRITICAL");
                DeveloperScansScanItemsViolatingPoliciesView view2 = new DeveloperScansScanItemsViolatingPoliciesView();
                view2.setPolicyName("vulnerability_policy");
                view2.setPolicySeverity("CRITICAL");
                DeveloperScansScanItemsViolatingPoliciesView view3 = new DeveloperScansScanItemsViolatingPoliciesView();
                view3.setPolicyName("license_policy");
                view3.setPolicySeverity("MINOR");
                DeveloperScansScanItemsViolatingPoliciesView view4 = new DeveloperScansScanItemsViolatingPoliciesView();
                view4.setPolicyName("other_policy");
                view4.setPolicySeverity("BLOCKER");

                violatingPolicies.add(view);
                violatingPolicies.add(view2);
                violatingPolicies.add(view3);
                violatingPolicies.add(view4);
                return violatingPolicies;
            }

            @Override
            public List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> getPolicyViolationVulnerabilities() {
                List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> vulnerabilities = new ArrayList<>();
                DeveloperScansScanItemsPolicyViolationVulnerabilitiesView view = new DeveloperScansScanItemsPolicyViolationVulnerabilitiesView() {
                    @Override
                    public String getName() {
                        return "Vulnerability violation";
                    }

                    @Override
                    public String getDescription() {
                        return "Violation Description";
                    }

                    @Override
                    public List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView> getViolatingPolicies() {
                        List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView> violatingPolicies = new ArrayList<>();
                        
                        DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView view = new DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView();
                        view.setPolicyName("vulnerability_policy");
                        view.setPolicySeverity("CRITICAL");
                        
                        DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView view2 = new DeveloperScansScanItemsPolicyViolationVulnerabilitiesViolatingPoliciesView();
                        view2.setPolicyName("vulnerability_policy_warning");
                        view2.setPolicySeverity("MINOR");
                 
                        violatingPolicies.add(view);
                        violatingPolicies.add(view2);
                        return violatingPolicies;
                    }
                };
                vulnerabilities.add(view);
                return vulnerabilities;
            }

            @Override
            public List<DeveloperScansScanItemsPolicyViolationLicensesView> getPolicyViolationLicenses() {
                List<DeveloperScansScanItemsPolicyViolationLicensesView> licenses = new ArrayList<>();
                
                DeveloperScansScanItemsPolicyViolationLicensesView view = new DeveloperScansScanItemsPolicyViolationLicensesView() {
                    @Override
                    public String getName() {
                        return "License name";
                    }

                    @Override
                    public List<DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView> getViolatingPolicies() {
                        List<DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView> violatingPolicies = new ArrayList<>();
                        
                        DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView view = new DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView(); 
                        view.setPolicyName("license_policy");
                        view.setPolicySeverity("CRITICAL");
                        
                        DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView view2 = new DeveloperScansScanItemsPolicyViolationLicensesViolatingPoliciesView(); 
                        view2.setPolicyName("license_policy_warning");
                        view2.setPolicySeverity("MINOR");
                        
                        violatingPolicies.add(view);
                        violatingPolicies.add(view2);
                        return violatingPolicies;
                    }
                };
                licenses.add(view);
                return licenses;
            }
        };
    }

}
