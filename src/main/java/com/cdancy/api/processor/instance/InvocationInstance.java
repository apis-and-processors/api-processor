/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cdancy.api.processor.instance;

import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.cache.ProcessorCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy.
 */
public class InvocationInstance {
    
    private final Class clazz;
    private final ImmutableMap<String, Annotation> classAnnotations;
    private final String method;
    private final ImmutableMap<String, Annotation> methodAnnotations;
    private final ImmutableList<ParameterInstance> parameterInstanceCache;
    private final Object [] arguments;
    private final TypeToken returnType;
    
    @Nullable
    private final AbstractExecutionHandler executionHandler;
    
    @Nullable
    private final AbstractErrorHandler errorHandler;
        
    @Nullable
    private final AbstractFallbackHandler fallbackHandler;

    @Nullable
    private final AbstractResponseHandler responseHandler;
        
    private InvocationInstance(Class clazz, 
            ImmutableMap<String, Annotation> classAnnotations, 
            String method, 
            ImmutableMap<String, Annotation> methodAnnotations, 
            ImmutableList<ParameterInstance> parameterInstanceCache,
            Object [] arguments,
            TypeToken returnType,
            AbstractExecutionHandler executionHandler,
            AbstractErrorHandler errorHandler,
            AbstractFallbackHandler fallbackHandler,
            AbstractResponseHandler responseHandler) {
        this.clazz = clazz;
        this.classAnnotations = classAnnotations;
        this.method = method;
        this.methodAnnotations = methodAnnotations;
        this.parameterInstanceCache = parameterInstanceCache;
        this.arguments = arguments;
        this.returnType = returnType;
        this.executionHandler = executionHandler;
        this.errorHandler = errorHandler;
        this.fallbackHandler = fallbackHandler;
        this.responseHandler = responseHandler;
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
    
    /**
     * Create InvocationInstance from passed args.
     * 
     * @param classInstance the ClassInstance used for class annotations.
     * @param methodInstance the MethodInstance used for method annotations.
     * @param args runtime arguments passed to method.
     * @return newly created InvocationInstance.
     */
    public static InvocationInstance newInstanceFrom(ClassInstance classInstance, MethodInstance methodInstance, Object [] args) {
        
        Class<? extends AbstractExecutionHandler> executionHandler = (methodInstance.executionHandler() != null) 
                ? methodInstance.executionHandler() 
                : classInstance.executionHandler();
        Class<? extends AbstractErrorHandler> errorHandler = (methodInstance.errorHandler() != null) 
                ? methodInstance.errorHandler() 
                : classInstance.errorHandler();
        Class<? extends AbstractFallbackHandler> fallbackHandler = (methodInstance.fallbackHandler() != null) 
                ? methodInstance.fallbackHandler() 
                : classInstance.fallbackHandler();
        Class<? extends AbstractResponseHandler> responseHandler = (methodInstance.responseHandler() != null) 
                ? methodInstance.responseHandler() 
                : classInstance.responseHandler();
        
        return new InvocationInstance(classInstance.clazz(), 
                classInstance.annotations(), 
                methodInstance.method(), 
                methodInstance.annotations(), 
                methodInstance.parameterInstanceCache(),
                args,
                methodInstance.returnType(), 
                executionHandler != null ? ProcessorCache.typeFrom(executionHandler) : null, 
                errorHandler != null ? ProcessorCache.typeFrom(errorHandler) : null, 
                fallbackHandler != null ? ProcessorCache.typeFrom(fallbackHandler) : null, 
                responseHandler != null ? ProcessorCache.typeFrom(responseHandler) : null);
    }

    public AbstractExecutionHandler executionHandler() {
        return this.executionHandler;
    }

    public AbstractErrorHandler errorHandler() {
        return this.errorHandler;
    }

    public AbstractFallbackHandler fallbackHandler() {
        return this.fallbackHandler;
    }

    public AbstractResponseHandler responseHandler() {
        return this.responseHandler;
    }
}
