package com.yankaibang.plugin

import java.lang.annotation.Annotation

interface IStep {
    Set<? extends Class<? extends Annotation>> annotations()
    void process(Map<String, Set<Object>> elementsByAnnotation)
}