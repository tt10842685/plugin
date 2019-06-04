package com.yankaibang.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.yankaibang.plugin.bean.ClickAntiShake
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.JarClassPath
import javassist.NotFoundException
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ClickAntiShakeInject {

    private final def pool = ClassPool.getDefault()
    private final def paths = new ArrayList()
    private CtClass onClickListenerCtClass
    private CtClass viewCtClass
    private boolean makeClassComplete = false

    void makeClass(File desc, Project project) {
        if(makeClassComplete) return
        paths.add(pool.appendClassPath(desc.absolutePath))

        //project.android.bootClasspath 加入android.jar，否则找不到android相关的所有类
        def androidClassPath = pool.appendClassPath(project.android.bootClasspath[0].toString())
        makeClassImpl(desc, project)
        pool.removeClassPath(androidClassPath)
        makeClassComplete = true
    }

    void makeClassImpl(File desc, Project project) {
        ClickAntiShake clickAntiShake = project.extensions.getByName('clickAntiShake')
        if(clickAntiShake.antiMillis < 0) clickAntiShake = new ClickAntiShake()

        CtClass ctClass = pool.makeClass("com.yankaibang.plugin.ClickAntiShakeHelper")

        CtField ctField = CtField.make("private java.util.Map clickAntiShakeTimes = new java.util.HashMap();", ctClass)
        ctClass.addField(ctField)

        CtMethod ctMethod = CtNewMethod.make(
                "public boolean check(android.view.View v) {\n" +
                "   long currentTime = System.currentTimeMillis();\n" +
                "   Long preClickTime = (Long)clickAntiShakeTimes.get(v);\n" +
                "   long t = 0L;\n" +
                "   if(preClickTime != null) {\n" +
                "       t = currentTime - preClickTime.longValue();\n" +
                "   }\n" +
                "   if(t < " + clickAntiShake.antiMillis + " && t > 0L) {\n" +
                "       return false;\n" +
                "   } else {\n" +
                "       clickAntiShakeTimes.put(v, Long.valueOf(currentTime));\n" +
                "       return true;\n" +
                "   }\n" +
                "}", ctClass)
        ctClass.addMethod(ctMethod)

        ctClass.writeFile(desc.absolutePath)
        ctClass.detach()
    }

    boolean injectDir(DirectoryInput directoryInput, File desc, Project project) {
        paths.add(pool.appendClassPath(directoryInput.file.absolutePath))

        //project.android.bootClasspath 加入android.jar，否则找不到android相关的所有类
        def androidClassPath = pool.appendClassPath(project.android.bootClasspath[0].toString())
        processDir(directoryInput.file.absolutePath, desc.absolutePath)
        pool.removeClassPath(androidClassPath)
        return true
    }

    boolean injectJar(JarInput jarInput, File desc, Project project) {
        String jarInPath = jarInput.file.absolutePath
        String jarOutPath = desc.absolutePath
        paths.add(pool.appendClassPath(jarInPath))

        if(jarInput.name.startsWith("org.jetbrains.")) return false
        if(jarInput.name.startsWith("com.android.")) return false
        if(jarInput.name.startsWith("android.")) return false

        //project.android.bootClasspath 加入android.jar，否则找不到android相关的所有类
        def androidClassPath = pool.appendClassPath(project.android.bootClasspath[0].toString())
        processJar(jarInPath, jarOutPath)
        pool.removeClassPath(androidClassPath)
        return true
    }

    void clean() {
        paths.each {
            pool.removeClassPath(it)
        }
        paths.clear()
        makeClassComplete = false
    }

    private void processDir(String dirInPath, String dirOutPath) {
        File dir = new File(dirInPath)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                if (file.isFile()) {
                    File outPutFile = new File(dirOutPath + filePath.substring(dirInPath.length()))
                    Files.createParentDirs(outPutFile)
                    if (filePath.endsWith(".class")
                            && !filePath.contains('R$')
                            && !filePath.contains('R.class')
                            && !filePath.contains("BuildConfig.class")) {
                        FileInputStream inputStream = new FileInputStream(file)
                        FileOutputStream outputStream = new FileOutputStream(outPutFile)
                        processDir(inputStream, outputStream)
                    } else {
                        FileUtils.copyFile(file, outPutFile)
                    }
                }
            }
        }
    }

    private void processDir(InputStream input, OutputStream output) {
        try {
            transform(input, output)
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            close(input, output)
        }
    }

    private void processJar(String jarInPath, String jarOutPath) {
        Files.createParentDirs(new File(jarOutPath))
        ZipInputStream zis = null
        ZipOutputStream zos = null
        try {
            zis = new ZipInputStream(new FileInputStream(new File(jarInPath)))
            zos = new ZipOutputStream(new FileOutputStream(new File(jarOutPath)))
            processJar(zis, zos)
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            close(zis, zos)
        }
    }

    private void processJar(ZipInputStream zis, ZipOutputStream zos) {
        ArrayList entries = new ArrayList()
        ZipEntry entry = zis.getNextEntry()
        while (entry != null) {
            String fileName = entry.getName()
            if (!entries.contains(fileName)) {
                entries.add(fileName)
                zos.putNextEntry(new ZipEntry(fileName))
                if (!entry.isDirectory() && fileName.endsWith(".class")
                        && !fileName.contains('R$')
                        && !fileName.contains('R.class')
                        && !fileName.contains("BuildConfig.class"))
                    transform(zis, zos)
                else {
                    ByteStreams.copy(zis, zos)
                }
            }
            entry = zis.getNextEntry()
        }
    }

    private void transform(InputStream input, OutputStream output) {
        CtClass ctClass = pool.makeClass(input)
        play(ctClass)
        output.write(ctClass.toBytecode())
        ctClass.detach()
    }

    private boolean isOnClickListener(CtClass ctClass) {
        def onClickListenerCtClass = getOnClickListenerCtClass()
        return !ctClass.isInterface() && ctClass.subtypeOf(onClickListenerCtClass)
    }

    private void play(CtClass ctClass) {
        if(isOnClickListener(ctClass)) {
            println "ClickAntiShake: " + ctClass.name
            CtField ctField = CtField.make("private com.yankaibang.plugin.ClickAntiShakeHelper clickHelper = " +
                    "new com.yankaibang.plugin.ClickAntiShakeHelper();", ctClass)
            ctClass.addField(ctField)

            def method = ctClass.getDeclaredMethod("onClick", getViewCtClass())
            method.insertBefore("if(!clickHelper.check(\$1)) return;")
        }
    }

    private void close(Closeable... closeables) {
        closeables.each {
            try {
                if(it != null) {
                    it.close()
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }

    private CtClass getOnClickListenerCtClass() {
        if(onClickListenerCtClass != null) return onClickListenerCtClass
        onClickListenerCtClass = pool.get('android.view.View$OnClickListener')
        return onClickListenerCtClass
    }

    private CtClass getViewCtClass() {
        if(viewCtClass != null) return viewCtClass
        viewCtClass = pool.get('android.view.View')
        return viewCtClass
    }
}