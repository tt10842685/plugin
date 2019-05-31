package com.yankaibang.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

public abstract class BaseAnnotationPlugin implements Plugin<Project> {

    abstract protected Inject inject

    void apply(Project project) {
        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames)
        String module = project.path.replace(":", "")
        System.out.println("current module is " + module)
        project.android.registerTransform(new AnnotationTransform(project, inject))
    }
}