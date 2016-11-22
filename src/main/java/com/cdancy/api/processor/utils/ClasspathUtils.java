/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.utils;

/**
 *
 * @author cdancy
 */
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Sets;

import com.google.inject.Singleton;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import java.util.Set;

@Singleton
public class ClasspathUtils {
    
    public static Set<Class> classesAnnotatedWith(Class annotation) {
        checkNotNull(annotation, "class annotataion cannot be null");
        Set<Class> builtApis = Sets.newHashSet();
        new FastClasspathScanner().
                matchClassesWithAnnotation(annotation, c -> {
                    builtApis.add(c);
                }).scan();
        return builtApis;
    }
}
