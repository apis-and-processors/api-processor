/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.instance;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;

/**
 *
 * @author cdancy
 */
public class MethodInstance {
    
    private final String method;
    private final ImmutableMap<String, Annotation> methodAnnotations;
    private final ImmutableList<ParameterInstance> parameterInstanceCache;
    private final TypeToken returnType;
    
    public MethodInstance(String method, Annotation[] methodAnnotations, ImmutableList<Parameter> parameters, TypeToken returnType) {
        this.method = method;
        
        ImmutableMap.Builder<String, Annotation> mapBuilder = ImmutableMap.builder();
        Lists.newArrayList(methodAnnotations).stream().forEach( entry -> {
            mapBuilder.put(entry.getClass().getName(), entry);
        });
        this.methodAnnotations = mapBuilder.build();
        
        ImmutableList.Builder<ParameterInstance> listBuilder = ImmutableList.builder();
        parameters.stream().forEach( entry -> {
            Class clazz = entry.getType().getRawType();
            Annotation [] parameterAnnotations = entry.getAnnotations();
            final ParameterInstance parameterInstance = new ParameterInstance(clazz, parameterAnnotations);
            listBuilder.add(parameterInstance);
        });
        this.parameterInstanceCache = listBuilder.build();
        
        this.returnType = returnType;
    }
    
    public String method() {
        return method;
    }
    
    public ImmutableMap<String, Annotation> methodAnnotations() {
        return methodAnnotations;
    }
    
    public ImmutableList<ParameterInstance> parameterInstanceCache() {
        return parameterInstanceCache;
    }
    
    public TypeToken returnType() {
        return returnType;
    }
}
