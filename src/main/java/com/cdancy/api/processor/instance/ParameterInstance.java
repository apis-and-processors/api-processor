/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.instance;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.lang.annotation.Annotation;

/**
 *
 * @author cdancy
 */
public class ParameterInstance {
    
    private final Class clazz;
    private final ImmutableMap<String, Annotation> parameterAnnotations;
    
    public ParameterInstance(Class clazz, Annotation[] parameterAnnotations) {
        this.clazz = clazz;
        
        ImmutableMap.Builder<String, Annotation> mapBuilder = ImmutableMap.builder();
        Lists.newArrayList(parameterAnnotations).stream().forEach( entry -> {
            mapBuilder.put(entry.getClass().getName(), entry);
        });
        this.parameterAnnotations = mapBuilder.build();        
    }
    
    public Class clazz() {
        return clazz;    
    }
    
    public ImmutableMap<String, Annotation> parameterAnnotations() {
        return parameterAnnotations;
    }
    
    public <T> T getParameterAnnotation(Class<T> clazz) {
        Annotation anno = parameterAnnotations().get(clazz.getName());
        return (anno != null) ? clazz.cast(anno) : null;
    }
}
