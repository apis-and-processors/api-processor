/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import java.lang.reflect.InvocationHandler;
import java.util.Set;

/**
 *
 * @author cdancy
 */
public class ProcessorUtils {

    public static Set<Class> findClassesAnnotatedWith(Class annotation) {
        checkNotNull(annotation, "class annotataion cannot be null");
        Set<Class> builtApis = Sets.newHashSet();
        new FastClasspathScanner().
                matchClassesWithAnnotation(annotation, c -> {
                    builtApis.add(c);
                }).scan();
        return builtApis;
    }
    
    public static <T> T newTypeFrom(Class<T> proxyInterface, InvocationHandler invocationHandler) {
        checkNotNull(proxyInterface, "proxyInterface cannot be null");
        checkNotNull(invocationHandler, "invocationHandler cannot be null");
        return Reflection.newProxy(proxyInterface, invocationHandler);       
    }
}
