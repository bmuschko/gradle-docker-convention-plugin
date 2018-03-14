package com.bmuschko.gradle.docker

class DockerNodeJsApplication {
    String baseImage = 'node:9'
    Set<Integer> ports = [8080]
    String tag
}