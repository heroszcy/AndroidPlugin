package com.cici.plugin.manager

import com.cici.plugin.AutoConfigPlugin
import org.gradle.api.Project

import java.util.regex.Pattern

class ProjectModuleManager {
    static final String PLUGIN_NAME = AutoConfigPlugin.PLUGIN_NAME

    //为区别于组件单独以app方式运行的task，将组件module打包成aar时，在local.properties文件中添加 assemble_aar_for_cc_component=true
    static final String ASSEMBLE_AAR_FOR_CC_COMPONENT = "assemble_aar_for_cc_component"
    //组件单独以app方式运行时使用的测试代码所在目录(manifest/java/assets/res等),这个目录下的文件不会打包进主app
    static final String DEBUG_DIR = "src/main/debug/"
    //主app，一直以application方式编译
    static final String MODULE_MAIN_APP = "mainApp"
    //apply了cc-settings.gradle的module，但不是组件，而是一直作为library被其它组件依赖
    static final String MODULE_ALWAYS_LIBRARY = "alwaysLib"
    static String mainModuleName
    static boolean taskIsAssemble

    static boolean manageModule(Project project) {
        taskIsAssemble = false
        mainModuleName = null
        //local.properties
        Properties localProperties = new Properties()
        try {
            def localFile = project.rootProject.file('local.properties')
            if (localFile != null && localFile.exists()) {
                localProperties.load(localFile.newDataInputStream())
            }
        } catch (Exception ignored) {
            println("${PLUGIN_NAME}: local.properties not found")
        }
        initByTask(project)
        def mainApp = isMainApp(project)
        def assembleFor = isAssembleFor(project)
        def buildingAar = isBuildingAar(localProperties)
        def alwaysLib = isAlwaysLib(project)
        boolean runAsApp = false
        if (mainApp) {
            runAsApp = true
        } else if (alwaysLib || buildingAar) {
            runAsApp = false
        } else if (assembleFor || !taskIsAssemble) {
            runAsApp = true
        }
        project.ext.runAsApp = runAsApp
        println "${PLUGIN_NAME}: project=${project.name}, runAsApp=${runAsApp} . taskIsAssemble:${taskIsAssemble}. " +
                "settings(mainApp:${mainApp}, alwaysLib:${alwaysLib}, assembleThisModule:${assembleFor}, buildingAar:${buildingAar})"
        if (runAsApp) {
            project.apply plugin: 'com.android.application'

//            project.android.sourceSets.main {
//                //debug模式下，如果存在src/main/debug/AndroidManifest.xml，则自动使用其作为manifest文件
//                def debugManifest = "${DEBUG_DIR}AndroidManifest.xml"
//                if (project.file(debugManifest).exists()) {
//                    manifest.srcFile debugManifest
//                }
//                //debug模式下，如果存在src/main/debug/assets，则自动将其添加到assets源码目录
//                if (project.file("${DEBUG_DIR}assets").exists()) {
//                    assets.srcDirs = ['src/main/assets', "${DEBUG_DIR}assets"]
//                }
//                //debug模式下，如果存在src/main/debug/java，则自动将其添加到java源码目录
//                if (project.file("${DEBUG_DIR}java").exists()) {
//                    java.srcDirs = ['src/main/java', "${DEBUG_DIR}java"]
//                }
//                //debug模式下，如果存在src/main/debug/res，则自动将其添加到资源目录
//                if (project.file("${DEBUG_DIR}res").exists()) {
//                    res.srcDirs = ['src/main/res', "${DEBUG_DIR}res"]
//                }
//            }
        } else {
            project.apply plugin: 'com.android.library'
        }
        return runAsApp
    }

    //需要集成打包相关的task
    static final String TASK_TYPES = ".*((((ASSEMBLE)|(BUILD)|(INSTALL)|((BUILD)?TINKER)|(RESGUARD)).*)|(ASR)|(ASD))"
    static void initByTask(Project project) {
        def taskNames = project.gradle.startParameter.taskNames
        def allModuleBuildApkPattern = Pattern.compile(TASK_TYPES)
        for (String task : taskNames) {
            if (allModuleBuildApkPattern.matcher(task.toUpperCase()).matches()) {
                taskIsAssemble = true
                if (task.contains(":")) {
                    def arr = task.split(":")
                    mainModuleName = arr[arr.length - 2].trim()
                }
                break
            }
        }
    }

    /**
     * 当前是否正在给指定的module集成打包
     */
    static boolean isAssembleFor(Project project) {
        return project.name == mainModuleName
    }
    static boolean isMainApp(Project project) {
        return project.ext.has(MODULE_MAIN_APP) && project.ext.mainApp
    }
    static boolean isAlwaysLib(Project project) {
        return project.ext.has(MODULE_ALWAYS_LIBRARY) && project.ext.alwaysLib
    }
    //判断当前设置的环境是否为组件打aar包（比如将组件打包上传maven库）
    static boolean isBuildingAar(Properties localProperties) {
        return 'true' == localProperties.getProperty(ASSEMBLE_AAR_FOR_CC_COMPONENT)
    }
}
