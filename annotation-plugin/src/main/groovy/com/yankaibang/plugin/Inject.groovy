package com.yankaibang.plugin

import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import org.gradle.api.Project

import java.lang.annotation.Annotation

class Inject {

    private final def pool = ClassPool.getDefault()
    private final def annotationClasses = new HashSet<CtClass>()
    private final def annotationElementMap = new HashMap<String, HashSet<Object>>()
    private final def annotations = new HashSet<String>()
    private final def steps

    Inject(IStep... steps) {
        this.steps = steps
        steps.each { IStep step ->
            annotations.addAll(step.annotations())
        }
    }

    void injectDir(String path, Project project) {
        //project.android.bootClasspath 加入android.jar，否则找不到android相关的所有类
        def androidClassPath = pool.appendClassPath(project.android.bootClasspath[0].toString())
        pool.appendClassPath(path)
        findAnnotationElements(path)
        processAnnotationElements(project)
        writeAnnotationClasses(path)
        clearAnnotationElements()
        pool.removeClassPath(androidClassPath)
    }

    private void clearAnnotationElements() {
        annotationClasses.each {
            it.detach()
        }
        annotationClasses.clear()
        annotationElementMap.clear()
    }

    private void processAnnotationElements(Project project) {
        steps.each { IStep step ->
            def stepAnnotationElementMap = new HashMap<String, HashSet<Object>>()
            step.annotations().each {String annotation ->
                def elements = annotationElementMap.get(annotation)
                if(elements != null) {
                    stepAnnotationElementMap.put(annotation, elements)
                }
            }
            if(!stepAnnotationElementMap.isEmpty()) {
                step.process(stepAnnotationElementMap)
            }
        }
    }

    private void writeAnnotationClasses(String path) {
        annotationClasses.each {
            it.writeFile(path)
        }
    }

    private void findAnnotationElements(String path) {
        File dir = new File(path)
        dir.eachFileRecurse { File file ->
            String filePath = file.absolutePath
            if (file.isFile()
                    && filePath.endsWith(".class")
                    && !filePath.contains('R$')
                    && !filePath.contains('R.class')
                    && !filePath.contains("BuildConfig.class")) {
                System.out.println("file name is " + filePath)
                findAnnotationElements(file)
            }
        }
    }

    private void findAnnotationElements(File file) {
        def ctClass = getClass(file)
        if(ctClass == null) return

        def find = false
        find |= findAnnotationElements(ctClass, annotations)
        find |= findAnnotationElements(ctClass.getMethods(), annotations)
        find |= findAnnotationElements(ctClass.getFields(), annotations)

        if(find) {
            annotationClasses.add(ctClass)
        } else {
            ctClass.detach()
        }
    }

    private CtClass getClass(File file) {
        FileInputStream inputStream = null
        try {
            inputStream = new FileInputStream(file)
            return pool.makeClass(inputStream)
        } catch (Exception e) {
            if(inputStream != null) {
                inputStream.close()
            }
        }
        return null
    }

    private boolean findAnnotationElements(CtClass ctClass, Set<String> annotationSet) {
        def find = false
        def annotations = ctClass.getAnnotations()
        annotations.each {Annotation annotation ->
            if(annotationSet.contains(getClassName(annotation))) {
                addAnnotationElement(annotation, ctClass)
                find = true
            }
        }
        return find
    }

    private boolean findAnnotationElements(CtMethod[] ctMethods, Set<String> annotationSet) {
        def find = false
        ctMethods.each {CtMethod ctMethod ->
            def annotationList = new ArrayList<Annotation>()
            def annotations = ctMethod.getAnnotations()
            def parameterAnnotations = ctMethod.getParameterAnnotations()

            annotationList.addAll(annotations)
            parameterAnnotations.each {
                annotationList.addAll(it)
            }

            annotationList.each {Annotation annotation ->
                if(annotationSet.contains(getClassName(annotation))) {
                    addAnnotationElement(annotation, ctMethod)
                    find = true
                }
            }
        }
        return find
    }

    private boolean findAnnotationElements(CtField[] ctFields, Set<String> annotationSet) {
        def find = false
        ctFields.each {CtField ctField ->
            def annotations = ctField.getAnnotations()
            annotations.each {Annotation annotation ->
                if(annotationSet.contains(getClassName(annotation))) {
                    addAnnotationElement(annotation, ctField)
                    find = true
                }
            }
        }
        return find
    }

    private addAnnotationElement(Annotation annotation, Object element) {
        def annotationElements = getAnnotationElements(getClassName(annotation), annotationElementMap)
        annotationElements.add(element)
    }

    private HashSet<Object> getAnnotationElements(String annotation,
                                                  HashMap<String, HashSet<Object>> annotationElementMap) {
        def annotationElements = annotationElementMap.get(annotation)
        if(annotationElements == null) {
            annotationElements = new HashSet<Object>()
            annotationElementMap.put(annotation, annotationElements)
        }
        return annotationElements
    }

    private String getClassName(Object ctClass) {
        return ctClass.toString()
    }
}