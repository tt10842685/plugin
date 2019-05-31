package com.yankaibang.plugin.bean

class JarInfo {
    String name
    boolean fullMatchName = false
    List<String> delClasses = new ArrayList()

    JarInfo(String name) {
        this.name = name
    }

    def classDelete(String className){
        delClasses.add(className)
    }


    @Override
    public String toString() {
        return "JarInfo{" +
                "name='" + name + '\'' +
                ", fullMatchName=" + fullMatchName +
                ", delClasses=" + delClasses +
                '}';
    }
}