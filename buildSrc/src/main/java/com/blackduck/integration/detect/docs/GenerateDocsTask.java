package com.blackduck.integration.detect.docs;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import com.google.gson.Gson;
import com.blackduck.integration.detect.docs.copied.HelpJsonData;
import com.blackduck.integration.detect.docs.copied.HelpJsonDetectorRule;
import com.blackduck.integration.detect.docs.copied.HelpJsonOption;
import com.blackduck.integration.detect.docs.model.DeprecatedPropertyTableGroup;
import com.blackduck.integration.detect.docs.model.Detectable;
import com.blackduck.integration.detect.docs.model.DetectorStatusCodes;
import com.blackduck.integration.detect.docs.model.SimplePropertyTableGroup;
import com.blackduck.integration.detect.docs.model.SplitGroup;
import com.blackduck.integration.detect.docs.pages.AdvancedPropertyTablePage;
import com.blackduck.integration.detect.docs.pages.DeprecatedPropertyTablePage;
import com.blackduck.integration.detect.docs.pages.DetectorCascadePage;
import com.blackduck.integration.detect.docs.pages.ExitCodePage;
import com.blackduck.integration.detect.docs.pages.SimplePropertyTablePage;
import com.blackduck.integration.exception.IntegrationException;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public class GenerateDocsTask extends DefaultTask {
    private static final String DITAMAP_TEMPLATE_FILENAME = "ditamap.ftl";
    private static final String DITAMAP_OUTPUT_FILENAME = "detect.ditamap";

    @TaskAction
    public void generateDocs() throws IOException, TemplateException, IntegrationException {
        Project docsProject = getProject();

        File helpJsonFile = new File(docsProject.getBuildDir(), "detect-" + docsProject.getVersion() + "-help.json");
        Reader reader = new FileReader(helpJsonFile);
        HelpJsonData helpJson = new Gson().fromJson(reader, HelpJsonData.class);

        File docsProjectDir = docsProject.getProjectDir();
        File docsSrcMainDir = new File(docsProjectDir, "src/main");
        File docsSrcMainResourcesDir = new File(docsSrcMainDir, "resources");

        File sourceMarkdownDir = new File(docsSrcMainDir, "markdown");
        File outputDir = new File(docsProject.getBuildDir(), "generated");

        File runningDir = new File(outputDir, "runningdetect");
		File installDir = new File(outputDir, "downloadingandinstalling");
        File troubleshootingDir = new File(outputDir, "troubleshooting");

        FileUtils.deleteDirectory(outputDir);
        troubleshootingDir.mkdirs();

        File templatesDir = new File(docsSrcMainResourcesDir, "templates");
        TemplateProvider templateProvider = new TemplateProvider(templatesDir, docsProject.getVersion().toString());

        // Prepare ditamap files
        createFromFreemarker(templateProvider, DITAMAP_TEMPLATE_FILENAME, new File(outputDir, DITAMAP_OUTPUT_FILENAME));
        FileUtils.copyFileToDirectory(new File(docsProjectDir, "topics.ditamap"), outputDir);
        FileUtils.copyFileToDirectory(new File(docsProjectDir, "keywords.ditamap"), outputDir);

        FileUtils.copyDirectory(sourceMarkdownDir, outputDir);
        createMarkdownFromFreemarker(templateProvider, troubleshootingDir, "exit-codes", new ExitCodePage(helpJson.getExitCodes()));
        createMarkdownFromFreemarker(templateProvider, runningDir, "status-file", new DetectorStatusCodes(helpJson.getDetectorStatusCodes()));

        handleProperties(templateProvider, outputDir, helpJson);
        createFromFreemarker(templateProvider, "downloadlocations.ftl", new File(installDir, "downloadlocations.md"));
    }

    private void createMarkdownFromFreemarker(TemplateProvider templateProvider, File outputDir, String templateName, Object data) throws IOException, TemplateException {
        createFromFreemarker(templateProvider, String.format("%s.ftl", templateName), new File(outputDir, String.format("%s.md", templateName)), data);
    }

    private void createFromFreemarker(TemplateProvider templateProvider, String templateRelativePath, File to) throws IOException, TemplateException {
        createFromFreemarker(templateProvider, templateRelativePath, to, new HashMap<String, String>(0));
    }

    private void createFromFreemarker(TemplateProvider templateProvider, String templateRelativePath, File to, Object data) throws IOException, TemplateException {
        to.getParentFile().mkdirs();
        Template template = templateProvider.getTemplate(templateRelativePath);
        try (Writer writer = new FileWriter(to)) {
            template.process(data, writer);
        }
    }
	
    private String encodePropertyLocation(String propertyName) {
        if (!propertyName.equals(propertyName.trim())) {
            throw new RuntimeException(
                "Property name should not include trim-able white space. Property (" + propertyName + ") should be shortened to (" + propertyName.trim() + ") ");
        }
        Map<String, String> supportedCharacters = new HashMap<>();
        String literals = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        for (char literalCharacter : literals.toCharArray()) {
            supportedCharacters.put(String.valueOf(literalCharacter), String.valueOf(literalCharacter));
        }
        supportedCharacters.put(" ", "-");
        supportedCharacters.put(".", "46");
        supportedCharacters.put("(", "40");
        supportedCharacters.put(")", "41");
        StringBuilder encoded = new StringBuilder();
        for (char character : propertyName.toCharArray()) {
            String charString = String.valueOf(character);
            if (!supportedCharacters.containsKey(charString)) {
                throw new RuntimeException(String.format(
                    "Unsupported character literal in property name, please add it to supported characters or remove the character (%s) in (%s) ",
                    character,
                    propertyName
                ));
            } else {
                encoded.append(supportedCharacters.get(charString));
            }
        }
        return encoded.toString().toLowerCase();
    }

    private String makeLinkSafe(String linkText) {
        return linkText.replace(" ", "-");
    }

    private void handleProperties(TemplateProvider templateProvider, File outputDir, HelpJsonData helpJson) throws IntegrationException, IOException, TemplateException {
        Map<String, String> superGroups = createSuperGroupLookup(helpJson);

        // example: superGroup/key
        Map<String, String> groupLocations = superGroups.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, it -> String.format("%s/%s", it.getValue(), it.getKey()).toLowerCase()));

        // Updating the location on all the json options so that a new object with only 1 new property did not have to be created (and then populated) from the existing.
        for (HelpJsonOption helpJsonOption : helpJson.getOptions()) {
            String groupLocation = getGroupLocation(groupLocations, makeLinkSafe(helpJsonOption.getGroup()));
            String encodedPropertyLocation = encodePropertyLocation(helpJsonOption.getPropertyName());
            helpJsonOption.setLocation(String.format("%s.md#%s", groupLocation, encodedPropertyLocation)); //ex: superGroup/key/#property_name
        }

        Map<String, List<HelpJsonOption>> groupedOptions = helpJson.getOptions().stream()
            .collect(Collectors.groupingBy(o -> makeLinkSafe(o.getGroup())));

        List<SplitGroup> splitGroupOptions = new ArrayList<>();
        for (Map.Entry<String, List<HelpJsonOption>> group : groupedOptions.entrySet()) {
            List<HelpJsonOption> deprecated = group.getValue().stream()
                .filter(HelpJsonOption::getDeprecated)
                .collect(Collectors.toList());

            List<HelpJsonOption> simple = group.getValue().stream()
                .filter(helpJsonObject -> !deprecated.contains(helpJsonObject)
                    && (StringUtils.isBlank(helpJsonObject.getCategory()) || "simple".equals(helpJsonObject.getCategory()))
                )
                .collect(Collectors.toList());

            List<HelpJsonOption> advanced = group.getValue().stream()
                .filter(it -> !simple.contains(it) && !deprecated.contains(it))
                .collect(Collectors.toList());

            String superGroupName = getOrThrow(superGroups, group.getKey(), String.format("Missing super group: %s", group.getKey()));
            String groupLocation = getGroupLocation(groupLocations, group.getKey());

            // TODO: We only have to do this because we added "(Advanced)" to the section header of advanced properties. Putting property info in the name is not scalable and leads to fixes like this.
            advanced.forEach(property -> property.setLocation(String.format("%s-advanced", property.getLocation())));
            deprecated.forEach(property -> property.setLocation(String.format("%s-deprecated", property.getLocation())));

            SplitGroup splitGroup = new SplitGroup(group.getKey(), superGroupName, groupLocation, simple, advanced, deprecated);
            splitGroupOptions.add(splitGroup);
        }

        File propertiesFolder = new File(outputDir, "properties");
        for (SplitGroup group : splitGroupOptions) {
            File superGroupFolder = new File(propertiesFolder, group.getSuperGroup().toLowerCase());
            File targetMarkdown = new File(superGroupFolder, group.getGroupName() + ".md");
            createFromFreemarker(templateProvider, "property-group.ftl", targetMarkdown, group);
        }

        List<SimplePropertyTableGroup> simplePropertyTableData = new ArrayList<>();
        for (SplitGroup splitGroupOption : splitGroupOptions) {
            if (splitGroupOption.getSimple().isEmpty()) {
                continue;
            }

            String groupLocation = getGroupLocation(groupLocations, splitGroupOption.getGroupName());
            SimplePropertyTableGroup simplePropertyTableGroup = new SimplePropertyTableGroup(splitGroupOption.getGroupName(), groupLocation, splitGroupOption.getSimple());
            simplePropertyTableData.add(simplePropertyTableGroup);
        }

        List<DeprecatedPropertyTableGroup> deprecatedPropertyTableData = new ArrayList<>();
        for (SplitGroup splitGroupOption : splitGroupOptions) {
            if (splitGroupOption.getDeprecated().isEmpty()) {
                continue;
            }

            String groupLocation = getGroupLocation(groupLocations, splitGroupOption.getGroupName());
            DeprecatedPropertyTableGroup deprecatedPropertyTableGroup = new DeprecatedPropertyTableGroup(
                splitGroupOption.getGroupName(),
                groupLocation,
                splitGroupOption.getDeprecated()
            );
            deprecatedPropertyTableData.add(deprecatedPropertyTableGroup);
        }

        createMarkdownFromFreemarker(templateProvider, propertiesFolder, "basic-properties", new SimplePropertyTablePage(simplePropertyTableData));
        createMarkdownFromFreemarker(templateProvider, propertiesFolder, "deprecated-properties", new DeprecatedPropertyTablePage(deprecatedPropertyTableData));
        createMarkdownFromFreemarker(templateProvider, propertiesFolder, "all-properties", new AdvancedPropertyTablePage(splitGroupOptions));
    }

    private String getGroupLocation(Map<String, String> groupLocationMap, String group) throws IntegrationException {
        return getOrThrow(groupLocationMap, makeLinkSafe(group), String.format("Missing group location: %s", group));
    }

    private <K, V> V getOrThrow(Map<K, V> map, K key, String missingMessage) throws IntegrationException {
        return Optional.ofNullable(map.get(key))
            .orElseThrow(() -> new IntegrationException(missingMessage));
    }

    // Technically each key has exactly 1 super key (but this is not enforced in the json) so here we check that assumption and return the mapping.
    // TODO: Add a new object to the helpJson which is a super key lookup so that the super key lookup is not just embedded in the object and then we don't have to do this at all.
    // TODO: Add the default "Configuration" to the help json instead of blank and having this populate.
    private Map<String, String> createSuperGroupLookup(HelpJsonData helpJson) {
        Map<String, String> lookup = new HashMap<>();

        helpJson.getOptions().forEach(option -> {
            String optionGroup = makeLinkSafe(option.getGroup());
            final String defaultSuperGroup = "Configuration";
            String rawSuperGroup = option.getSuperGroup();
            String superGroup;
            if (StringUtils.isBlank(rawSuperGroup)) {
                superGroup = defaultSuperGroup;
            } else {
                superGroup = rawSuperGroup;
            }

            if (lookup.containsKey(optionGroup) && !superGroup.equals(lookup.get(optionGroup))) {
                throw new RuntimeException(String.format(
                    "The created detect help JSON had a key '%s' whose super key '%s' did not match a different options super key in the same key '%s'.",
                    optionGroup,
                    superGroup,
                    lookup.get(optionGroup)
                ));
            } else if (!lookup.containsKey(optionGroup)) {
                lookup.put(optionGroup, superGroup);
            }
        });

        return lookup;
    }
}

