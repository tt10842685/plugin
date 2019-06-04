package com.yankaibang.plugin

import com.yankaibang.plugin.bean.ClickAntiShake

import org.gradle.BuildResult
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ClickAntiShakePlugin implements Plugin<Project> {

    void apply(Project project) {
        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames)
        String module = project.path.replace(":", "")
        System.out.println("current module is " + module)

        ClickAntiShake clickAntiShake = new ClickAntiShake()
        project.extensions.add('clickAntiShake', clickAntiShake)

        def transform = new ClickAntiShakeTransform(project)
        project.android.registerTransform(transform)
        project.gradle.buildFinished { BuildResult buildResult ->
            transform.clean()
        }
    }
}