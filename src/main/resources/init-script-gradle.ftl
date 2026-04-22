import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.lang.String;

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

Set<String> projectNameExcludeFilter = convertStringToSet('${excludedProjectNames?replace("\\", "\\\\")?replace("\'", "\\\'")}')
Set<String> projectNameIncludeFilter = convertStringToSet('${includedProjectNames?replace("\\", "\\\\")?replace("\'", "\\\'")}')
Set<String> projectPathExcludeFilter = convertStringToSet('${excludedProjectPaths?replace("\\", "\\\\")?replace("\'", "\\\'")}')
Set<String> projectPathIncludeFilter = convertStringToSet('${includedProjectPaths?replace("\\", "\\\\")?replace("\'", "\\\'")}')
Boolean rootOnly = Boolean.parseBoolean("${rootOnlyOption}")

gradle.allprojects {
    // Trigger dependency gathering for each project
    task gatherDependencies(type: DefaultTask) {
        def projectName = project.name
        doLast {
            println "Gathering dependencies for " + projectName
        }
    }

    afterEvaluate { currentProject ->
        // Capture project properties during configuration phase.
        // Configuration enumeration is deferred to gradle.projectsEvaluated
        // so that lazily-registered configs (AGP, KSP, etc.) are visible.
        def projectPath = currentProject.path
        def projectName = currentProject.name
        def isRootProject = isRoot(currentProject)
        def extractionDir = System.getProperty('GRADLEEXTRACTIONDIR')
        def projectDir = currentProject.projectDir
        def projectDirPath = projectDir.canonicalPath

        def rootProject = currentProject.gradle.rootProject
        def rootProjectName = rootProject.name
        def rootProjectPath = rootProject.path
        def rootProjectGroup = rootProject.group.toString()
        def rootProjectVersion = rootProject.version.toString()
        def rootProjectDir = rootProject.projectDir
        def rootProjectDirPath = rootProjectDir.canonicalPath

        def projectGroup = currentProject.group.toString()
        def projectVersion = currentProject.version.toString()
        def projectParent = currentProject.parent ? currentProject.parent.toString() : "none"

        if (extractionDir == null) {
            throw new IllegalStateException("GRADLEEXTRACTIONDIR system property is not set")
        }

        // Output file path is deterministic; file is created in projectsEvaluated if included.
        def projectFilePathConfig = computeProjectFilePath(projectPath, extractionDir, rootProject)

        def dependenciesTask = currentProject.tasks.getByName('dependencies')

        dependenciesTask.doFirst {
            try {
                // Write root project metadata file
                if (isRootProject) {
                    try {
                        File outputDirectory = new File(extractionDir)
                        outputDirectory.mkdirs()
                        File rootOutputFile = new File(outputDirectory, 'rootProjectMetadata.txt')

                        def rootProjectMetadataPieces = []
                        rootProjectMetadataPieces.add('DETECT META DATA START')
                        rootProjectMetadataPieces.add("rootProjectDirectory:" + rootProjectDirPath)
                        rootProjectMetadataPieces.add("rootProjectPath:" + rootProjectPath)
                        rootProjectMetadataPieces.add("rootProjectGroup:" + rootProjectGroup)
                        rootProjectMetadataPieces.add("rootProjectName:" + rootProjectName)
                        rootProjectMetadataPieces.add("rootProjectVersion:" + rootProjectVersion)
                        rootProjectMetadataPieces.add('DETECT META DATA END')

                        rootOutputFile.text = rootProjectMetadataPieces.join('\n')
                    } catch (Exception e) {
                        println "ERROR while generating root project metadata: " + e.message
                        e.printStackTrace()
                        throw e
                    }
                }
            } catch (Exception e) {
                println "ERROR in dependencies doFirst: " + e.message
                e.printStackTrace()
                throw e
            }
        }

        dependenciesTask.doLast {
            try {
                // Append project metadata if the output file exists (i.e. project was included)
                File projectFile = new File(projectFilePathConfig)
                if (projectFile.exists()) {
                    def metaDataPieces = []
                    metaDataPieces.add('')
                    metaDataPieces.add('DETECT META DATA START')
                    metaDataPieces.add("rootProjectDirectory:" + rootProjectDirPath)
                    metaDataPieces.add("rootProjectGroup:" + rootProjectGroup)
                    metaDataPieces.add("rootProjectPath:" + rootProjectPath)
                    metaDataPieces.add("rootProjectName:" + rootProjectName)
                    metaDataPieces.add("rootProjectVersion:" + rootProjectVersion)
                    metaDataPieces.add("projectDirectory:" + projectDirPath)
                    metaDataPieces.add("projectGroup:" + projectGroup)
                    metaDataPieces.add("projectName:" + projectName)
                    metaDataPieces.add("projectVersion:" + projectVersion)
                    metaDataPieces.add("projectPath:" + projectPath)
                    metaDataPieces.add("projectParent:" + projectParent)
                    metaDataPieces.add('DETECT META DATA END')
                    metaDataPieces.add('')

                    projectFile << metaDataPieces.join('\n')
                }
            } catch (Exception e) {
                println "ERROR in dependencies doLast: " + e.message
                e.printStackTrace()
                throw e
            }
        }

        // Force the dependencies task to run after gatherDependencies
        currentProject.gatherDependencies.finalizedBy(currentProject.tasks.getByName('dependencies'))
        currentProject.gatherDependencies
    }
}

// Enumerate configurations and set up output files after all projects are fully evaluated.
// This ensures lazily-registered configurations (AGP, KSP, ktlint, etc.) are visible.
gradle.projectsEvaluated {
    gradle.allprojects { currentProject ->
        def dependenciesTask = currentProject.tasks.findByName('dependencies')
        if (!dependenciesTask) return

        def extractionDir = System.getProperty('GRADLEEXTRACTIONDIR')
        def projectPath = currentProject.path
        def rootProject = currentProject.gradle.rootProject
        def projectName = currentProject.name
        def isRootProject = isRoot(currentProject)
        def isPhantom = !currentProject.buildFile.exists()

        def projectMatchesFilters = (rootOnly && isRootProject) ||
            (!rootOnly && shouldInclude(projectNameExcludeFilter, projectNameIncludeFilter, projectName) &&
             shouldInclude(projectPathExcludeFilter, projectPathIncludeFilter, projectPath))

        def configurationNames = getFilteredConfigurationNames(
            currentProject,
            '${excludedConfigurationNames?replace("\\", "\\\\")?replace("\'", "\\\'")}',
            '${includedConfigurationNames?replace("\\", "\\\\")?replace("\'", "\\\'")}'
        )

        def selectedConfigs = []
        configurationNames.each { name ->
            try {
                def config = currentProject.configurations.findByName(name)
                if (config) selectedConfigs.add(config)
            } catch (Exception e) {
                println "Could not process configuration: " + name
                throw e
            }
        }

        def shouldIncludeProject = projectMatchesFilters && (!selectedConfigs.isEmpty() || isPhantom)

        // Phantoms and excluded projects get an empty config set to avoid unnecessary resolution
        if (isPhantom || !projectMatchesFilters) {
            dependenciesTask.configurations = [] as Set
        } else {
            dependenciesTask.configurations = selectedConfigs as Set
        }

        // Create output file only for included projects; doLast uses existence as the inclusion signal
        if (shouldIncludeProject) {
            def projectFilePathConfig = computeProjectFilePath(projectPath, extractionDir, rootProject)
            File projectFile = new File(projectFilePathConfig)
            if (projectFile.exists()) projectFile.delete()
            projectFile.createNewFile()
            try {
                dependenciesTask.outputFile = projectFile
                println "Set output file during projectsEvaluated to " + projectFile.getAbsolutePath()
            } catch (Exception e) {
                println "Could not set outputFile property: " + e.message
                e.printStackTrace()
                throw e
            }
        }
    }
}

// ## START methods invoked by tasks above
<#-- Do not parse with Freemarker because Groovy variable replacement in template strings is the same as Freemarker template syntax. -->
<#noparse>
def isRoot(Project project) {
    try {
        // Only the root project has path ":"
        return project.path == ":"
    } catch (Exception e) {
        println "ERROR in isRoot: " + e.message
        e.printStackTrace()
        throw e
    }
}


// Get path for project file
def computeProjectFilePath(String projectPath, String outputDirectoryPath, Project rootProject) {
    try {
        File outputDirectory = createTaskOutputDirectory(outputDirectoryPath)
        String name = projectPath ?: ""

        int depthCount = name.split(':').length - 1
        // Root project path ":" has one colon but depth is 0
        if (projectPath == ":") {
            depthCount = 0
        }
        String depth = String.valueOf(depthCount)

        String finalName
        if (projectPath == ":") {
            String rootProjectName = rootProject.getName().replaceAll("[^\\p{IsAlphabetic}\\p{Digit}]+", "_")
            finalName = "root_project_${rootProjectName}"
        } else {
            String nameForFile = name.substring(1) // Remove leading ':'
            nameForFile = nameForFile.replaceAll("[^\\p{IsAlphabetic}\\p{Digit}]+", "_")
            finalName = "project_" + nameForFile
        }

        if (finalName.endsWith("_")) {
            finalName = finalName.substring(0, finalName.length() - 1)
        }

        return new File(outputDirectory, "${finalName}__depth${depth}_dependencyGraph.txt").getAbsolutePath()
    } catch (Exception e) {
        println "ERROR in computeProjectFilePath: " + e.message
        e.printStackTrace()
        throw e
    }
}

// Returns filtered configuration names to avoid serialization issues with Configuration objects
def getFilteredConfigurationNames(Project project, String excludedConfigurationNames, String includedConfigurationNames) {
    try {
        Set<String> configurationExcludeFilter = convertStringToSet(excludedConfigurationNames)
        Set<String> configurationIncludeFilter = convertStringToSet(includedConfigurationNames)
        Set<String> filteredNames = new TreeSet<String>()

        for (def configuration : project.configurations) {
            try {
                if (shouldInclude(configurationExcludeFilter, configurationIncludeFilter, configuration.name)) {
                    filteredNames.add(configuration.name)
                }
            } catch (Exception e) {
                println "ERROR processing configuration " + configuration.name + ": " + e.message
                throw e
            }
        }

        return filteredNames
    } catch (Exception e) {
        println "ERROR in getFilteredConfigurationNames: " + e.message
        e.printStackTrace()
        throw e
    }
}

def createTaskOutputDirectory(String outputDirectoryPath) {
    File outputDirectory = new File(outputDirectoryPath)
    outputDirectory.mkdirs()

    outputDirectory
}

def shouldInclude(Set<String> excluded, Set<String> included, String value) {
    return !containsWithWildcard(value, excluded) && (included.isEmpty() || containsWithWildcard(value, included))
}

def convertStringToSet(String value) {
    return value.tokenize(',').toSet()
}

def containsWithWildcard(String value, Set<String> tokenSet) {
    for (String token : tokenSet) {
        if (match(value, token)) {
            return true
        }
    }
    return tokenSet.contains(value)
}

def match(String value, String token) {
    def tokenRegex = wildCardTokenToRegexToken(token)
    return value.matches(tokenRegex)
}

def wildCardTokenToRegexToken(String token) {
    Matcher matcher = Pattern.compile(/[^*?]+|(\*)|(\?)/).matcher(token)
    StringBuffer buffer= new StringBuffer()
    while (matcher.find()) {
        if(matcher.group(1) != null) {
            matcher.appendReplacement(buffer, '.*')
        } else if (matcher.group(2) != null) {
            matcher.appendReplacement(buffer, ".");
        } else {
            matcher.appendReplacement(buffer, '\\\\Q' + matcher.group(0) + '\\\\E')
        }
    }
    matcher.appendTail(buffer)
    return buffer.toString()
}
</#noparse>
// ## END methods invoked by tasks above
