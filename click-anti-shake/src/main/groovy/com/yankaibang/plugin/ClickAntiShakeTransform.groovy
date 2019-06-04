package com.yankaibang.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.Sets
import javassist.ClassPool
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class ClickAntiShakeTransform extends Transform {

    private Project project
    private ClickAntiShakeInject inject = new ClickAntiShakeInject()

    ClickAntiShakeTransform(Project project) {
        this.project = project
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        ClassPool.default.importPackage()

        System.out.println("project " + project.path.replace(":", ""))

        def makeClassDest = transformInvocation.outputProvider.getContentLocation("makeClass",
                TransformManager.CONTENT_CLASS,
                TransformManager.PROJECT_ONLY, Format.DIRECTORY)
        inject.makeClass(makeClassDest, project)

        transformInvocation.inputs.each { TransformInput input ->
            //对类型为jar文件的input进行遍历
            input.jarInputs.each { JarInput jarInput ->
                //jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                System.out.println("jarName " + jarName)
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                if(!inject.injectJar(jarInput, dest, project)) {
                    //将输入内容复制到输出
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->
                System.out.println("directoryInput " + directoryInput.file.absolutePath)
                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)

                if(!inject.injectDir(directoryInput, dest, project)) {
                    // 将input的目录复制到output指定目录
                    FileUtils.copyDirectory(directoryInput.file, dest)
                }
            }
        }
    }

    void clean() {
        inject.clean()
    }

    @Override
    String getName() {
        return "ClickAntiShakeTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_JARS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

}