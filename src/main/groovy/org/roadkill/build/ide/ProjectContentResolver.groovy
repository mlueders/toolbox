package org.roadkill.build.ide

import static groovy.io.FileType.FILES
import static groovy.io.FileType.DIRECTORIES
import org.roadkill.xml.XmlNodeStringTranslator

abstract class ProjectContentResolver {

    abstract List collectProjectDependencyNames()

    abstract List collectLibraryDependencyFiles()

    abstract List collectTestPaths()

    abstract List collectSrcPaths()


    private File projectDir
    private XmlNodeStringTranslator translator = new XmlNodeStringTranslator()

    ProjectContentResolver(File projectDir) {
        this.projectDir = projectDir
    }

    String getEclipseProjectName() {
        projectDir.name
    }

    String getIdeaModuleName() {
        projectDir.name
    }

    String getIdeaModuleLibraryName() {
        String projectName = getIdeaModuleName()
        "${projectName}-lib"
    }

    File getEclipseClasspathFile() {
        new File(projectDir, ".classpath")
    }

    File getEclipseProjectFile() {
        new File(projectDir, ".project")
    }

    File getIdeaModuleFile() {
        File moduleFile = null

        projectDir.eachFile(FILES, { File file ->
            if (file.name.endsWith(".iml")) {
                moduleFile = file
            }
        })

        if (moduleFile == null) {
            String moduleName = getIdeaModuleName()
            moduleFile = new File(projectDir, "${moduleName}.iml")
        }
        moduleFile
    }

    private File resolveIdeaProjectLibraryDirectory() {
        File parent = projectDir.parentFile
        File ideaProjectLibraryDirectory = null

        while ((ideaProjectLibraryDirectory == null) && (parent != null)) {
            File ideaProjectDirectory = new File(parent, ".idea")
            if (ideaProjectDirectory.exists()) {
                ideaProjectLibraryDirectory = new File(ideaProjectDirectory, "libraries")
            }
        }

        if (ideaProjectLibraryDirectory == null) {
            throw new RuntimeException("Failed to resolve idea project directory (.idea), must be in some parent of ${projectDir.absolutePath}")
        }
        ideaProjectLibraryDirectory
    }

    File getIdeaModuleLibraryFile() {
        File ideaProjectLibaryDirectory = resolveIdeaProjectLibraryDirectory()
        String moduleLibraryFileName = getIdeaModuleLibraryName().replace('-', '_') + ".xml"
        new File(ideaProjectLibaryDirectory, moduleLibraryFileName)
    }

    List collectLibraryDependencyDirectoryPaths() {
        Set libraryDependencyDirectoryPaths = []

        collectLibraryDependencyFiles().each { File file ->
            libraryDependencyDirectoryPaths << file.parentFile.absolutePath - "${projectDir.absolutePath}/"
        }
        libraryDependencyDirectoryPaths as List
    }

    List collectLibraryDependencyPaths() {
        collectLibraryDependencyFiles().collect { File file ->
            file.absolutePath - "${projectDir.absolutePath}/"
        }
    }

    List collectSrcAndTestPaths() {
        collectSrcPaths() + collectTestPaths()
    }

}