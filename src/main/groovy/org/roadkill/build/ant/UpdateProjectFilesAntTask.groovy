package org.roadkill.build.ant

import org.apache.tools.ant.Task
import org.apache.tools.ant.BuildException
import org.roadkill.build.ide.ProjectFileUpdater
import org.roadkill.build.ide.ProjectContentResolver
import org.roadkill.build.ide.EclipseProjectFileUpdater
import org.roadkill.build.ide.IdeaProjectFileUpdater

class UpdateProjectFilesAntTask extends Task {

    private File projectDir
    private String refreshType
    private String projectType

    void setProjectDir(File projectDir) {
        this.projectDir = projectDir
    }

    void setProjectType(String projectType) {
        this.projectType = projectType
    }

    void setRefreshType(String refreshType) {
        this.refreshType = refreshType
    }

    private void assertPropertiesValid() {
        if (projectDir == null) {
            throw new BuildException("Project directory was not set in the build file, must be qualified directory of target project")
        }
        if (!projectDir.exists()) {
            throw new BuildException("Project directory ${projectDir.absolutePath} does not exist")
        }
        if (projectType == null) {
            throw new BuildException("Project type (project.type) was not set in build file, must be one of [Eclipse Idea]")
        }
        if (refreshType == null) {
            throw new BuildException("Refresh type (refresh.type) was not set in build file, must be one of [update clean]")
        }
    }

    private ProjectFileUpdater resolveProjectFileUpdater() {
        ProjectContentResolver resolver = createContenResolver()
        switch (projectType) {
            case 'Eclipse': return new EclipseProjectFileUpdater(resolver)
            case 'Idea': return new IdeaProjectFileUpdater(resolver)
            default: throw new BuildException("Unknown project type '${projectType}'")
        }
    }

    private ProjectContentResolver createContenResolver() {
        throw new RuntimeException('implement')
    }

    private boolean isCleanRefresh() {
        refreshType.toLowerCase() == "clean"
    }

    @Override
    void execute() throws BuildException {
        assertPropertiesValid()

        // TODO: need to write to ant log
        println "Updating project files for project ${projectDir.name}, refreshType=${refreshType}"

        ProjectFileUpdater projectFileUpdater = resolveProjectFileUpdater()
        if (isCleanRefresh()) {
            projectFileUpdater.deleteProjectFiles()
        }
        projectFileUpdater.updateProjectFiles()
    }
}
