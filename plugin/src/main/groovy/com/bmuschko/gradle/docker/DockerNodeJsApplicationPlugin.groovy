package com.bmuschko.gradle.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

class DockerNodeJsApplicationPlugin implements Plugin<Project> {
    static final String NODE_JS_APPLICATION_EXTENSION_NAME = 'nodeJsApplication'
    public static final String DOCKERFILE_TASK_NAME = 'createDockerfile'
    public static final String SYNC_DIST_RESOURCES_TASK_NAME = 'syncNodeFiles'
    public static final String BUILD_IMAGE_TASK_NAME = 'buildImage'
    public static final String PUSH_IMAGE_TASK_NAME = 'pushImage'
    
    @Override
    void apply(Project project) {
        project.apply(plugin: DockerRemoteApiPlugin)
        
        DockerExtension dockerExtension = project.extensions.getByType(DockerExtension)
        DockerNodeJsApplication dockerNodeJsApplication = dockerExtension.extensions.create(NODE_JS_APPLICATION_EXTENSION_NAME, DockerNodeJsApplication)
        
        Dockerfile createDockerfileTask = createDockerfileTask(project)
        Sync distSyncTask = createDistCopyResourcesTask(project, createDockerfileTask, dockerNodeJsApplication)
        createDockerfileTask.dependsOn distSyncTask
        DockerBuildImage dockerBuildImageTask = createBuildImageTask(project, createDockerfileTask, dockerNodeJsApplication)
        createPushImageTask(project, dockerBuildImageTask)
    }
    
    private Dockerfile createDockerfileTask(Project project, DockerNodeJsApplication dockerNodeJsApplication) {
        project.task(DOCKERFILE_TASK_NAME, type: Dockerfile) {
            description = 'Creates the Docker image for the Node.js application.'
            from { dockerNodeJsApplication.baseImage }
            copyFile('package*.json', './')
            copyFile('index.js', '/index.js')
            runCommand('npm install')
            entryPoint('node', 'index.js')
            exposePort { dockerJavaApplication.ports }
        }
    }
    
    private Sync createDistSyncResourcesTask(Project project, Dockerfile createDockerfileTask) {
        project.task(SYNC_DIST_RESOURCES_TASK_NAME, type: Sync) {
            description = "Copies the distribution resources to a temporary directory for image creation."
            from('.') {
                include 'package*.json'
            }
            from 'src/node'
            into createDockerfileTask.destFile.parentFile
        }
    }
    
    private DockerBuildImage createBuildImageTask(Project project, Dockerfile createDockerfileTask, DockerNodeJsApplication dockerNodeJsApplication) {
        project.task(BUILD_IMAGE_TASK_NAME, type: DockerBuildImage) {
            description = 'Builds the Docker image for the Java application.'
            dependsOn createDockerfileTask
            conventionMapping.inputDir = { createDockerfileTask.destFile.parentFile }
            conventionMapping.tag = { dockerNodeJsApplication.getTag() }
        }
    }
    
    private void createPushImageTask(Project project, DockerBuildImage dockerBuildImageTask) {
        project.task(PUSH_IMAGE_TASK_NAME, type: DockerPushImage) {
            description = 'Pushes created Docker image to the repository.'
            dependsOn dockerBuildImageTask
            conventionMapping.imageName = { dockerBuildImageTask.getTag() }
        }
    }
}