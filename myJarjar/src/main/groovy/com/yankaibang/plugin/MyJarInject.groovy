package com.yankaibang.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.yankaibang.plugin.bean.JarInfo
import com.yankaibang.plugin.bean.MyJar
import javassist.ClassPool
import javassist.JarClassPath
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MyJarInject {

    private final def pool = ClassPool.getDefault()
    private final def paths = new ArrayList()

    boolean injectDir(DirectoryInput directoryInput, File desc, Project project) {
        paths.add(pool.appendClassPath(directoryInput.file.absolutePath))
        return false
    }

    boolean injectJar(JarInput jarInput, File desc, Project project) {
        String jarInPath = jarInput.file.absolutePath
        String jarOutPath = desc.absolutePath
        paths.add(pool.appendClassPath(new JarClassPath(jarInPath)))

        def jarInfo = getJarInfo(jarInput, project)
        if(jarInfo == null) return false

        //project.android.bootClasspath 加入android.jar，否则找不到android相关的所有类
        def androidClassPath = pool.appendClassPath(project.android.bootClasspath[0].toString())
        process(jarInPath, jarOutPath, jarInfo)
        pool.removeClassPath(androidClassPath)
        return true
    }

    void clean() {
        paths.each {
            pool.removeClassPath(it)
        }
        paths.clear()
    }

    private JarInfo getJarInfo(JarInput jarInput, Project project) {
        MyJar myJar = project.extensions.getByName('myJar')
        if(myJar == null) return null
        def name = jarInput.name
        myJar.jarInfo.find {
            it.fullMatchName ? name == it.name : name.contains(it.name)
        }
    }

    private void process(String jarInPath, String jarOutPath, JarInfo jarInfo) {
        println jarInfo
        Files.createParentDirs(new File(jarOutPath))
        ZipInputStream zis = null
        ZipOutputStream zos = null
        try {
            zis = new ZipInputStream(new FileInputStream(new File(jarInPath)))
            zos = new ZipOutputStream(new FileOutputStream(new File(jarOutPath)))
            process(zis, zos, jarInfo)
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            close(zis, zos)
        }
    }

    private void process(ZipInputStream zis, ZipOutputStream zos, JarInfo jarInfo) {
        ArrayList entries = new ArrayList()
        ZipEntry entry = zis.getNextEntry()
        while (entry != null) {
            String fileName = entry.getName()
            if (!entries.contains(fileName)) {
                entries.add(fileName)
                if (!entry.isDirectory() && fileName.endsWith(".class")
                        && !fileName.contains('R$')
                        && !fileName.contains('R.class')
                        && !fileName.contains("BuildConfig.class"))
                    transform(entry, zis, zos, jarInfo)
                else {
                    zos.putNextEntry(new ZipEntry(fileName))
                    ByteStreams.copy(zis, zos)
                }
            }
            entry = zis.getNextEntry()
        }
    }

    private void transform(ZipEntry entry, ZipInputStream input, ZipOutputStream out, JarInfo jarInfo) {
        def name = entry.name.replaceAll("/", ".")
        if(jarInfo.delClasses.any { name.contains(it) }) {
            println "remove " + name
        } else {
            out.putNextEntry(new ZipEntry(entry.name))
            ByteStreams.copy(input, out)
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
}