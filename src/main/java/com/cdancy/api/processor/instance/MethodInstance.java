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
 * @author cdancy.
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
    
    /**
     * Create MethodInstance from passed args.
     * 
     * @param method name of method this instance is based on.
     * @param annotations annotations set on method.
     * @param parameters parameters set on method.
     * @param returnType the ReturnType that this method is based on.
     */
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
