/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.instance;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;

/**
 *
 * @author cdancy
 */
public class AnnotatedInstance {
    
    private final Class clazz;
    private final ImmutableMap<String, Annotation> classAnnotations;
    private final String method;
    private final ImmutableMap<String, Annotation> methodAnnotations;
    private final ImmutableList<ParameterInstance> parameterInstanceCache;
    private final Object [] arguments;
    private final TypeToken returnType;
        
    public AnnotatedInstance(Class clazz, 
            ImmutableMap<String, Annotation> classAnnotations, 
            String method, 
            ImmutableMap<String, Annotation> methodAnnotations, 
            ImmutableList<ParameterInstance> parameterInstanceCache,
            Object [] arguments,
            TypeToken returnType) {
        this.clazz = clazz;
        this.classAnnotations = classAnnotations;
        this.method = method;
        this.methodAnnotations = methodAnnotations;
        this.parameterInstanceCache = parameterInstanceCache;
        this.arguments = arguments;
        this.returnType = returnType;
    }
    
    public Class clazz() {
        return clazz;
    }
    
    public ImmutableMap<String, Annotation> classAnnotations() {
        return classAnnotations;
    } 
    
    public <T> T getClassAnnotation(Class<T> clazz) {
        Annotation anno = classAnnotations().get(clazz.getName());
        return (anno != null) ? clazz.cast(anno) : null;
    }
    
    public String method() {
        return method;
    }
    
    public ImmutableMap<String, Annotation> methodAnnotations() {
        return methodAnnotations;
    } 
    
    public <T> T getMethodAnnotation(Class<T> clazz) {
        Annotation anno = methodAnnotations().get(clazz.getName());
        return (anno != null) ? clazz.cast(anno) : null;
    }
    
    public ParameterInstance parameterInstance(int index) {
        return parameterInstanceCache.get(index);
    }
    
    public Object arguments(int index) {
        return arguments[index];
    }
    
    public TypeToken returnType() {
        return returnType;
    }
    
    public static AnnotatedInstance newInstanceFrom(ClassInstance classInstance, MethodInstance methodInstance, Object [] args) {
        return new AnnotatedInstance(classInstance.clazz(), 
                classInstance.annotations(), 
                methodInstance.method(), 
                methodInstance.methodAnnotations(), 
                methodInstance.parameterInstanceCache(),
                args,
                methodInstance.returnType());
    }
}
