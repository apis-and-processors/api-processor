/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.instance;

import com.cdancy.api.processor.proxy.InvokableCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 * @author cdancy
 */
public class ClassInstance {
    
    private final Class clazz;
    private final ImmutableMap<String, Annotation> annotations;
    private final Map<String, MethodInstance> methodInstanceCache = Maps.newConcurrentMap();

    public ClassInstance(Class clazz) {
        this.clazz = clazz;
        ImmutableMap.Builder<String, Annotation> mapBuilder = ImmutableMap.builder();
        Lists.newArrayList(clazz.getAnnotations()).stream().forEach( entry -> {
            mapBuilder.put(entry.getClass().getName(), entry);
        });
        this.annotations = mapBuilder.build();
    }
    
    public Class clazz() {
        return clazz;
    }
    
    public ImmutableMap<String, Annotation> annotations() {
        return annotations;
    }
    
    public <T> T getAnnotation(Class<T> clazz) {
        Annotation anno = annotations.get(clazz.getName());
        return (anno != null) ? clazz.cast(anno) : null;
    }
    
    public MethodInstance get(Class clazz, Method method) {
        MethodInstance methodInstance = methodInstanceCache.get(method.getName());
        if (methodInstance == null) {
            Invokable invokable = InvokableCache.invokable(clazz, method);            
            final MethodInstance newMethodInstance = new MethodInstance(invokable.getName(), invokable.getAnnotations(), invokable.getParameters(), invokable.getReturnType());
            methodInstanceCache.put(method.getName(), newMethodInstance);
            methodInstance = newMethodInstance;
        } 
        return methodInstance;
    }
}
