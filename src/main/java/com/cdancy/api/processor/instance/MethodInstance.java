/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.instance;

import com.cdancy.api.processor.annotations.ErrorHandler;
import com.cdancy.api.processor.annotations.ExecutionHandler;
import com.cdancy.api.processor.annotations.FallbackHandler;
import com.cdancy.api.processor.annotations.ResponseHandler;
import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.handlers.ProcessorHandles;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;

/**
 *
 * @author cdancy
 */
public class MethodInstance implements ProcessorHandles {
    
    private final String method;
    private final ImmutableMap<String, Annotation> annotations;
    private final ImmutableList<ParameterInstance> parameterInstanceCache;
    private final TypeToken returnType;
    
    private final Class<? extends AbstractExecutionHandler> executionHandler;
    private final Class<? extends AbstractErrorHandler> errorHandler;
    private final Class<? extends AbstractFallbackHandler> fallbackHandler;
    private final Class<? extends AbstractResponseHandler> responseHandler;
    
    public MethodInstance(String method, Annotation[] annotations, ImmutableList<Parameter> parameters, TypeToken returnType) {
        this.method = method;
        
        ImmutableMap.Builder<String, Annotation> mapBuilder = ImmutableMap.builder();
        for (Annotation methodAnnotation : annotations) {
            mapBuilder.put(methodAnnotation.annotationType().getName(), methodAnnotation);
        }
        this.annotations = mapBuilder.build();
        
        Class localExecutionHandler = null;
        Class localErrorHandler = null;
        Class localFallbackHandler = null;
        Class localResponseHandler = null;
        
        Annotation possibleAnnotation = this.annotations.get(ExecutionHandler.class.getName());
        if (possibleAnnotation != null) {
            ExecutionHandler anno = (ExecutionHandler)possibleAnnotation;
            localExecutionHandler = anno.value();
        }
        possibleAnnotation = this.annotations.get(ErrorHandler.class.getName());
        if (possibleAnnotation != null) {
            ErrorHandler anno = (ErrorHandler)possibleAnnotation;
            localErrorHandler = anno.value();
        }
        possibleAnnotation = this.annotations.get(FallbackHandler.class.getName());
        if (possibleAnnotation != null) {
            FallbackHandler anno = (FallbackHandler)possibleAnnotation;
            localFallbackHandler = anno.value();
        }
        possibleAnnotation = this.annotations.get(ResponseHandler.class.getName());
        if (possibleAnnotation != null) {
            ResponseHandler anno = (ResponseHandler)possibleAnnotation;
            localResponseHandler = anno.value();
        }
        
        this.executionHandler = localExecutionHandler;
        this.errorHandler = localErrorHandler;
        this.fallbackHandler = localFallbackHandler;
        this.responseHandler = localResponseHandler;
        
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
    
    public ImmutableMap<String, Annotation> annotations() {
        return annotations;
    }
    
    public ImmutableList<ParameterInstance> parameterInstanceCache() {
        return parameterInstanceCache;
    }
    
    public TypeToken returnType() {
        return returnType;
    }

    @Override
    public Class<? extends AbstractExecutionHandler> executionHandler() {
        return this.executionHandler;
    }

    @Override
    public Class<? extends AbstractErrorHandler> errorHandler() {
        return this.errorHandler;
    }

    @Override
    public Class<? extends AbstractFallbackHandler> fallbackHandler() {
        return this.fallbackHandler;
    }

    @Override
    public Class<? extends AbstractResponseHandler> responseHandler() {
        return this.responseHandler;
    }
}
