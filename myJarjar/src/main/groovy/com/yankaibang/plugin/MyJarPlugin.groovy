package com.yankaibang.plugin

import com.yankaibang.plugin.bean.JarInfo
import com.yankaibang.plugin.bean.MyJar
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project

public class MyJarPlugin implements Plugin<Project> {

    void apply(Project project) {
        NamedDomainObjectContainer<JarInfo> jarInfo = project.container(JarInfo)
        MyJar myJar = new MyJar(jarInfo)
        project.extensions.add('myJar', myJar)

        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames)
        String module = project.path.replace(":", "")
        System.out.println("current module is " + module)
        project.android.registerTransform(new MyJarTransform(project))
    }
}