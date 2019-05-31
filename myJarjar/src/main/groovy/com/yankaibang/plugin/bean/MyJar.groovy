package com.yankaibang.plugin.bean

import org.gradle.api.NamedDomainObjectContainer

class MyJar {
    NamedDomainObjectContainer<JarInfo> jarInfo

    MyJar(NamedDomainObjectContainer<JarInfo> jarInfo) {
        this.jarInfo = jarInfo
    }

    def jarInfo(Closure closure){
        jarInfo.configure(closure)
    }
}