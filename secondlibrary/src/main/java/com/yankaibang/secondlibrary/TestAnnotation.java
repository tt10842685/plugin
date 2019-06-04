package com.yankaibang.secondlibrary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by yankaibang on 2017/9/25.
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface TestAnnotation {
}
