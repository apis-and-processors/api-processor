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
    private final ImmutableMap<String, Annotation> annotations;
    
    public ParameterInstance(Class clazz, Annotation[] annotations) {
        this.clazz = clazz;
        
        ImmutableMap.Builder<String, Annotation> mapBuilder = ImmutableMap.builder();
        Lists.newArrayList(annotations).stream().forEach( entry -> {
            mapBuilder.put(entry.annotationType().getName(), entry);

        });
        this.annotations = mapBuilder.build();        
    }
    
    public Class clazz() {
        return clazz;    
    }
    
    public ImmutableMap<String, Annotation> annotations() {
        return annotations;
    }
    
    public <T> T getParameterAnnotation(Class<T> clazz) {
        Annotation anno = annotations().get(clazz.getName());
        return (anno != null) ? clazz.cast(anno) : null;
    }
}
