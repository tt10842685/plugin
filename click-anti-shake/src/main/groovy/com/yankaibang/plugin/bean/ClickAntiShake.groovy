package com.yankaibang.plugin.bean

class ClickAntiShake {
    public static final long DEFAULT_ANTI_MILLIS = 300
    long antiMillis = DEFAULT_ANTI_MILLIS
    List<String> excludeJarNames = new ArrayList()
    List<String> matchJarNames = new ArrayList()

    def excludeJarName(String name){
        excludeJarNames.add(name)
    }

    def matchJarName(String name){
        matchJarNames.add(name)
    }
}