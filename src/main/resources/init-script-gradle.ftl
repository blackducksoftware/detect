import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.lang.String;

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

Set<String> projectNameExcludeFilter = convertStringToSet("""${excludedProjectNames}""")
Set<String> projectNameIncludeFilter = convertStringToSet("""${includedProjectNames}""")
Set<String> projectPathExcludeFilter = convertStringToSet("""${excludedProjectPaths}""")
Set<String> projectPathIncludeFilter = convertStringToSet("""${includedProjectPaths}""")
Boolean rootOnly = Boolean.parseBoolean("${rootOnlyOption}")

gradle.allprojects {
    // add a new task to each project to start the process of getting the dependencies
    task gatherDependencies(type: DefaultTask) {
        // Store project name during configuration phase
        def projectName = project.name
        doLast {
            println "Gathering dependencies for " + projectName
        }
    }

    afterEvaluate { currentProject ->
        // Capture all needed project properties during configuration
        def projectPath = currentProject.path
        def projectName = currentProject.name
        def isRootProject = isRoot(currentProject)
        def extractionDir = System.getProperty('GRADLEEXTRACTIONDIR')
        def projectDir = currentProject.projectDir
        def projectDirPath = projectDir.canonicalPath

        // Root project properties
        def rootProject = currentProject.gradle.rootProject
        def rootProjectName = rootProject.name
        def rootProjectPath = rootProject.path
        def rootProjectGroup = rootProject.group.toString()
        def rootProjectVersion = rootProject.version.toString()
        def rootProjectDir = rootProject.projectDir
        def rootProjectDirPath = rootProjectDir.canonicalPath

        // Project metadata
        def projectGroup = currentProject.group.toString()
        def projectVersion = currentProject.version.toString()
        def projectParent = currentProject.parent ? currentProject.parent.toString() : "none"

        // Prepare configuration names
        def configurationNames = getFilteredConfigurationNames(currentProject,
            """${excludedConfigurationNames}""", """${includedConfigurationNames}""")

        def selectedConfigs = []
        configurationNames.each { name ->
            try {
                def config = currentProject.configurations.findByName(name)
                if (config) {
                    selectedConfigs.add(config)
                }
            } catch (Exception e) {
                println "Could not process configuration: " + name
                throw e
            }
        }

        // Check if the project should be included in results
        def shouldIncludeProject = (rootOnly && isRootProject) ||
            (!rootOnly && shouldInclude(projectNameExcludeFilter, projectNameIncludeFilter, projectName) &&
             shouldInclude(projectPathExcludeFilter, projectPathIncludeFilter, projectPath))

        // Capture output file path during configuration
        def projectFilePathConfig = computeProjectFilePath(projectPath, extractionDir, rootProject)

        // Configure the dependencies task during configuration time
        def dependenciesTask = currentProject.tasks.getByName('dependencies')

        // Set the configurations at configuration time if possible
        if (!selectedConfigs.isEmpty()) {
            dependenciesTask.configurations = selectedConfigs
        }

        // Set the output file at configuration time if possible
        if (shouldIncludeProject) {
            // Create output file directly during configuration
            File projectFile = new File(projectFilePathConfig)
            if (projectFile.exists()) {
                projectFile.delete()
            }
            projectFile.createNewFile()

            // Set the output file during configuration phase
            try {
                dependenciesTask.outputFile = projectFile
                println "Set output file during configuration to " + projectFile.getAbsolutePath()
            } catch (Exception e) {
                println "Could not set outputFile property during configuration: " + e.message
                e.printStackTrace()
                throw e
            }
        }

        dependenciesTask.doFirst {
            try {
                if (extractionDir == null) {
                    throw new IllegalStateException("GRADLEEXTRACTIONDIR system property is not set")
                }

                // Create metadata file for root project
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
                if(shouldIncludeProject) {
                    File projectFile = new File(projectFilePathConfig)

                    // Add metadata at the end of the file
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

                    // Append to file
                    projectFile << metaDataPieces.join('\n')
                }
            } catch (Exception e) {
                println "ERROR in dependencies doLast: " + e.message
                e.printStackTrace()
                throw e
            }
        }

        // This forces the dependencies task to be run
        currentProject.gatherDependencies.finalizedBy(currentProject.tasks.getByName('dependencies'))
        currentProject.gatherDependencies
    }
}

// ## START methods invoked by tasks above
<#-- Do not parse with Freemarker because Groovy variable replacement in template strings is the same as Freemarker template syntax. -->
<#noparse>
def isRoot(Project project) {
    try {
        Project rootProject = project.gradle.rootProject;
        return project.name.equals(rootProject.name)
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
        // The root project path is ":" which has one colon, but its depth should be 0.
        if (projectPath == ":") {
            depthCount = 0
        }
        String depth = String.valueOf(depthCount)

        String finalName
        // Aggressive sanitization for both root and subprojects
        if (projectPath == ":") {
            String rootProjectName = rootProject.getName().replaceAll("[^\\p{IsAlphabetic}\\p{Digit}]+", "_")
            finalName = "root_project_${rootProjectName}"
        } else {
            String nameForFile = name.substring(1) // Remove leading ':'
            nameForFile = nameForFile.replaceAll("[^\\p{IsAlphabetic}\\p{Digit}]+", "_")
            finalName = "project_" + nameForFile
        }

        // Ensure the final name does not end with an underscore before appending __depth
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

// Get only configuration names to avoid serialization issues
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
