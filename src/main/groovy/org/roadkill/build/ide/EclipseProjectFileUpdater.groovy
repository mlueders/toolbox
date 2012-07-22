package org.roadkill.build.ide

import org.roadkill.xml.XmlNodeStringTranslator

class EclipseProjectFileUpdater implements ProjectFileUpdater {

    private ProjectContentResolver resolver
    private XmlNodeStringTranslator translator = new XmlNodeStringTranslator()

    EclipseProjectFileUpdater(ProjectContentResolver resolver) {
        this.resolver = resolver
    }

    void deleteProjectFiles() {
        println "Deleting ${resolver.getEclipseProjectFile().absolutePath}"
        resolver.getEclipseProjectFile().delete()
        println "Deleting ${resolver.getEclipseClasspathFile().absolutePath}"
        resolver.getEclipseClasspathFile().delete()
    }

    void updateProjectFiles() {
        createOrUpdateClasspathFile()
        createProjectFileIfAbsent()
    }

    void createOrUpdateClasspathFile() {
        File classpathFile = resolver.getEclipseClasspathFile()
        Node classpathNode

        if (classpathFile.exists()) {
            println "Updating ${classpathFile.absolutePath}..."
            classpathNode = translator.getFileAsNodeIfExists(classpathFile)
        } else {
            println "No .classpath file, creating ${classpathFile.absolutePath}..."
            classpathNode = createClasspathNode()
        }

        updateClasspathWithLibraryDependencies(classpathNode)
        translator.writeNodeToFile(classpathNode, classpathFile)
    }
    
    private Node createClasspathNode() {
        Node classpath = new Node(null, 'classpath')
        classpath.appendNode('classpathentry',
            ['kind': 'con', 'path': 'org.eclipse.jdt.launching.JRE_CONTAINER'])
        classpath.appendNode('classpathentry', 
            ['kind': 'output', 'path': 'bin'])

        List srcAndTestPaths = resolver.collectSrcAndTestPaths()
        srcAndTestPaths.each { String path ->
            classpath.appendNode('classpathentry',
                ['kind': 'src', 'path': path])
        }

        List projectDependencyPaths = resolver.collectProjectDependencyNames()
        projectDependencyPaths.each { String path ->
            classpath.appendNode('classpathentry',
                ['kind': 'src', 'path': "/${path}", 'combineaccessrules': 'false'])

        }
        classpath
    }

    private void updateClasspathWithLibraryDependencies(Node classpath) {
        List libraryDependencyPaths = resolver.collectLibraryDependencyPaths()

        classpath.classpathentry.each { Node entry ->
            if (entry.'@kind' == "lib") {
                String path = entry.'@path'
                if (!libraryDependencyPaths.remove(path)) {
                    classpath.remove(entry)
                }
            }
        }
        
        libraryDependencyPaths.each { String path ->
            classpath.appendNode("classpathentry",
                ['kind': 'lib', 'path': path])
        }
    }
    
    private void createProjectFileIfAbsent() {
        File projectFile = resolver.getEclipseProjectFile()

        if (!projectFile.exists()) {
            println "No .project file, creating ${projectFile.absolutePath}..."
            Node projectNode = createProjectNode()
            translator.writeNodeToFile(projectNode, projectFile)
        }
    }

    private Node createProjectNode() {
        Node project = new Node(null, '.project')
        Node projectDescription = project.appendNode('projectDescription')
        projectDescription.appendNode('name', resolver.getEclipseProjectName())
        projectDescription.appendNode('comment')
        projectDescription.appendNode('projects')
        Node buildSpec = projectDescription.appendNode('buildSpec')
        Node buildCommand = buildSpec.appendNode('buildCommand')
        buildCommand.appendNode('name', 'org.eclipse.jdt.core.javabuilder')
        Node natures = projectDescription.appendNode('natures')
        natures.appendNode('nature', 'org.eclipse.jdt.core.javanature')
        project
    }

}