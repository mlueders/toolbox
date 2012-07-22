package org.roadkill.build.ide

import org.roadkill.build.ide.ProjectFileUpdater
import org.roadkill.build.ide.ProjectContentResolver
import org.roadkill.xml.XmlNodeStringTranslator

class IdeaProjectFileUpdater implements ProjectFileUpdater {

    private ProjectContentResolver resolver
    private XmlNodeStringTranslator translator = new XmlNodeStringTranslator()

    public IdeaProjectFileUpdater(ProjectContentResolver resolver) {
        this.resolver = resolver
    }

    void deleteProjectFiles() {
        println "Deleting ${resolver.getIdeaModuleFile().absolutePath}"
        resolver.getIdeaModuleFile().delete()
        println "Deleting ${resolver.getIdeaModuleLibraryFile().absolutePath}"
        resolver.getIdeaModuleLibraryFile().delete()
    }

    void updateProjectFiles() {
        createModuleFileIfAbsent()
        createModuleLibraryFileIfAbsent()
    }

    private void createModuleFileIfAbsent() {
        File moduleFile = resolver.getIdeaModuleFile()
        if (!moduleFile.exists()) {
            println "No module file, creating ${moduleFile.absolutePath}..."
            Node projectNode = createModuleNode()
            translator.writeNodeToFile(projectNode, moduleFile)
        }
    }

    private void createModuleLibraryFileIfAbsent() {
        File moduleLibraryFile = resolver.getIdeaModuleLibraryFile()

        if (!moduleLibraryFile.exists()) {
            println "No module library file, creating ${moduleLibraryFile.absolutePath}..."
            Node moduleLibraryNode = createModuleLibraryNode()
            translator.writeNodeToFile(moduleLibraryNode, moduleLibraryFile)
        }
    }

    private Node createModuleNode() {
        Node module = new Node(null, 'module',
                ['type': 'JAVA_MODULE',
                 'version': '4']
        )

        Node component = module.appendNode('component',
                ['name': 'NewModuleRootManager',
                 'inherit-compiler-output': 'true']
        )
        component.appendNode('exclude-output')
        Node content = component.appendNode('content', ['url': 'file://$MODULE_DIR$'])
        resolver.collectSrcPaths().each { String srcPath ->
            content.appendNode('sourceFolder',
                ['url': "file://\$MODULE_DIR\$/${srcPath}",
                 'isTestSource': 'false']
            )
        }
        resolver.collectTestPaths().each { String testPath ->
            content.appendNode('sourceFolder',
                ['url': "file://\$MODULE_DIR\$/${testPath}",
                'isTestSource': 'true'])
        }

        component.appendNode('orderEntry', ['type': 'inheritedJdk'])
        component.appendNode('orderEntry',
                ['type': 'sourceFolder',
                 'forTests': 'false']
        )

            resolver.collectProjectDependencyNames().each { String dependency ->
            component.appendNode('orderEntry', ['type': 'module', 'module-name': dependency])
        }

        String moduleLibraryName = resolver.getIdeaModuleLibraryName()
        component.appendNode('orderEntry',
                ['type': 'library', 'name': moduleLibraryName, 'level': 'project'])

        module
    }

    private Node createModuleLibraryNode() {
        String moduleName = resolver.getIdeaModuleName()
        String moduleLibraryName = resolver.getIdeaModuleLibraryName()

        List libDirPaths = resolver.collectLibraryDependencyDirectoryPaths()
        List libDirUrls = libDirPaths.collect { String path ->
            "file://\$PROJECT_DIR\$/${moduleName}/${path}"
        }

        Node component = new Node(null, 'component', ['name': 'libraryTable'])
        Node library = component.appendNode('library', ['name': moduleLibraryName])
        Node classes = library.appendNode('CLASSES')
        library.appendNode('JAVADOC')
        library.appendNode('SOURCES')

        libDirUrls.each { String url ->
            classes.appendNode('root', ['url': url])
            library.appendNode('jarDirectory', ['url': url])
        }
        component
    }

}
