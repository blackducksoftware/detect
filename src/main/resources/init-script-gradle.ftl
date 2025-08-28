import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.lang.String;

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

Set<String> projectNameExcludeFilter = convertStringToSet('${excludedProjectNames}')
Set<String> projectNameIncludeFilter = convertStringToSet('${includedProjectNames}')
Set<String> projectPathExcludeFilter = convertStringToSet('${excludedProjectPaths}')
Set<String> projectPathIncludeFilter = convertStringToSet('${includedProjectPaths}')
Boolean rootOnly = Boolean.parseBoolean("${rootOnlyOption}")
gradle.allprojects {
    // Add a new task to each project to start the process of getting the dependencies
    task gatherDependencies(type: DefaultTask) {
        doLast {
            println "Gathering dependencies for " + project.name
        }
    }

    // Create a custom task to configure and modify the dependencies task for each project
    task configureDependencies {
        doLast {
            // Set up the necessary properties for the dependencies task
            ext {
                excludedProjectNames = '${excludedProjectNames}'
                includedProjectNames = '${includedProjectNames}'
                excludedConfigurationNames = '${excludedConfigurationNames}'
                includedConfigurationNames = '${includedConfigurationNames}'
                outputDirectoryPath = System.getProperty('GRADLEEXTRACTIONDIR')
            }

            generateRootProjectMetaData(project, outputDirectoryPath)

            // Check if the dependencies task exists in the project
            def dependenciesTask = project.tasks.findByName('dependencies')
            if (dependenciesTask) {
                // Modify the dependencies task as needed
                dependenciesTask.doFirst {
                    if ((rootOnly && isRoot(project)) ||
                        (!rootOnly && shouldInclude(projectNameExcludeFilter, projectNameIncludeFilter, project.name) &&
                         shouldInclude(projectPathExcludeFilter, projectPathIncludeFilter, project.path))) {

                        File projectOutputFile = findProjectOutputFile(project, outputDirectoryPath)
                        File projectFile = createProjectOutputFile(projectOutputFile)

                        if (dependenciesTask.metaClass.respondsTo(dependenciesTask, "setConfigurations")) {
                            println "Updating configurations for task"
                            dependenciesTask.setConfigurations(filterConfigurations(project, excludedConfigurationNames, includedConfigurationNames))
                        } else {
                            println "Could not find method 'setConfigurations'"
                        }

                        if (dependenciesTask.metaClass.respondsTo(dependenciesTask, "setOutputFile")) {
                            println "Updating output file for task to " + projectFile.getAbsolutePath()
                            dependenciesTask.setOutputFile(projectFile)
                        } else {
                            println "Could not find method 'setOutputFile'"
                        }
                    } else {
                        println "Excluding from results subproject: " + project.path
                    }
                }

                dependenciesTask.doLast {
                    if ((rootOnly && isRoot(project)) ||
                        (!rootOnly && shouldInclude(projectNameExcludeFilter, projectNameIncludeFilter, project.name) &&
                         shouldInclude(projectPathExcludeFilter, projectPathIncludeFilter, project.path))) {
                        File projectFile = findProjectOutputFile(project, outputDirectoryPath)
                        appendProjectMetadata(project, projectFile)
                    }
                }
            } else {
                println "No 'dependencies' task found for project: " + project.name
            }
        }
    }

    // Finalize the gatherDependencies task to run before the configureDependencies task
    project.gatherDependencies.finalizedBy(configureDependencies)
}

// ## START methods invoked by tasks above
<#-- Do not parse with Freemarker because Groovy variable replacement in template strings is the same as Freemarker template syntax. -->
<#noparse>
def isRoot(Project project) {
    Project rootProject = project.gradle.rootProject;
    return project.name.equals(rootProject.name)
}

def generateRootProjectMetaData(Project project, String outputDirectoryPath) {
    File outputDirectory = createTaskOutputDirectory(outputDirectoryPath)
    outputDirectory.mkdirs()

    Project rootProject = project.gradle.rootProject;
    /* if the current project is the root project then generate the file containing
       the meta data for the root project otherwise ignore.
     */
    if (project.name.equals(rootProject.name)) {
        File rootOutputFile = new File(outputDirectory, 'rootProjectMetadata.txt');
        String rootProjectDirectory = rootProject.getProjectDir().getCanonicalPath()
        String rootProjectPath = rootProject.path.toString()
        String rootProjectGroup = rootProject.group.toString()
        String rootProjectName = rootProject.name.toString()
        String rootProjectVersionName = rootProject.version.toString()

        def rootProjectMetadataPieces = []
        rootProjectMetadataPieces.add('DETECT META DATA START')
        rootProjectMetadataPieces.add("rootProjectDirectory:${rootProjectDirectory}")
        rootProjectMetadataPieces.add("rootProjectPath:${rootProjectPath}")
        rootProjectMetadataPieces.add("rootProjectGroup:${rootProjectGroup}")
        rootProjectMetadataPieces.add("rootProjectName:${rootProjectName}")
        rootProjectMetadataPieces.add("rootProjectVersion:${rootProjectVersionName}")
        rootProjectMetadataPieces.add('DETECT META DATA END')
        rootOutputFile << rootProjectMetadataPieces.join('\n')
    }
}

def findProjectOutputFile(Project project, String outputDirectoryPath) {
    File outputDirectory = createTaskOutputDirectory(outputDirectoryPath)
    String name = project.toString()

    int depthCount = 0
    for(char c: name.toCharArray()) {
        if (c == ':') {
            depthCount++
        }
    }
    String depth = String.valueOf(depthCount)

    String nameForFile = name?.replaceAll(/[^\p{IsAlphabetic}\p{Digit}]/, "_")
    File outputFile = new File(outputDirectory, "${nameForFile}_depth${depth}_dependencyGraph.txt")

    outputFile
}

def filterConfigurations(Project project, String excludedConfigurationNames, String includedConfigurationNames) {
    Set<String> configurationExcludeFilter = convertStringToSet(excludedConfigurationNames)
    Set<String> configurationIncludeFilter = convertStringToSet(includedConfigurationNames)
    Set<Configuration> filteredConfigurationSet = new TreeSet<Configuration>(new Comparator<Configuration>() {
        public int compare(Configuration conf1, Configuration conf2) {
            return conf1.getName().compareTo(conf2.getName());
        }
    })
    for (Configuration configuration : project.configurations) {
        if (shouldInclude(configurationExcludeFilter, configurationIncludeFilter, configuration.name)) {
            filteredConfigurationSet.add(configuration)
        }
    }

    filteredConfigurationSet
}

def appendProjectMetadata(Project project, File projectOutputFile) {
    Project rootProject = project.gradle.rootProject;
    String rootProjectGroup = rootProject.group.toString()
    String rootProjectName = rootProject.name.toString()
    String rootProjectVersionName = rootProject.version.toString()
    String rootProjectPath = rootProject.path.toString()
    String group = project.group.toString()
    String name = project.name.toString()
    String version = project.version.toString()
    String path = project.path.toString()

    def metaDataPieces = []
    metaDataPieces.add('')
    metaDataPieces.add('DETECT META DATA START')
    metaDataPieces.add("rootProjectDirectory:${rootProject.getProjectDir().getCanonicalPath()}")
    metaDataPieces.add("rootProjectGroup:${rootProjectGroup}")
    metaDataPieces.add("rootProjectPath:${rootProjectPath}")
    metaDataPieces.add("rootProjectName:${rootProjectName}")
    metaDataPieces.add("rootProjectVersion:${rootProjectVersionName}")
    metaDataPieces.add("projectDirectory:${project.getProjectDir().getCanonicalPath()}")
    metaDataPieces.add("projectGroup:${group}")
    metaDataPieces.add("projectName:${name}")
    metaDataPieces.add("projectVersion:${version}")
    metaDataPieces.add("projectPath:${path}")
    metaDataPieces.add("projectParent:${project.parent}")
    metaDataPieces.add('DETECT META DATA END')
    metaDataPieces.add('')

    projectOutputFile << metaDataPieces.join('\n')
}

def createProjectOutputFile(File projectFile) {
    if (projectFile.exists()) {
        projectFile.delete()
    }

    projectFile.createNewFile()
    projectFile
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
