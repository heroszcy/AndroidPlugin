package com.cici.plugin

import com.cici.plugin.manager.ProjectModuleManager
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 自动切换配置application 和 library
 */
class AutoConfigPlugin implements Plugin<Project>{
    static final String PLUGIN_NAME = 'cici-auto-config'
    static final String EXT_NAME = 'autoConfig'
    @Override
    void apply(Project project) {
        println("project(${project.name}) apply ${PLUGIN_NAME} plugin")
//        project.extensions.create(EXT_NAME,AutoConfigExtension)
        ProjectModuleManager.manageModule(project)
    }

}
