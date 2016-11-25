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
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;

/**
 *
 * @author cdancy.
 */
public class ClassInstance implements ProcessorHandles {
    
    private final Class clazz;
    private final ImmutableMap<String, Annotation> annotations;

    private final Class<? extends AbstractExecutionHandler> executionHandler;
    private final Class<? extends AbstractErrorHandler> errorHandler;
    private final Class<? extends AbstractFallbackHandler> fallbackHandler;
    private final Class<? extends AbstractResponseHandler> responseHandler;

    /**
     * Create ClassInstance from passed class.
     * 
     * @param clazz Class definition.
     */
    public ClassInstance(Class clazz) {
        this.clazz = clazz;
        
        Class localExecutionHandler = null;
        Class localErrorHandler = null;
        Class localFallbackHandler = null;
        Class localResponseHandler = null;
        
        ImmutableMap.Builder<String, Annotation> mapBuilder = ImmutableMap.builder();
        for (Annotation clazzAnnotation : clazz.getAnnotations()) {
            mapBuilder.put(clazzAnnotation.annotationType().getName(), clazzAnnotation); 
        }
        this.annotations = mapBuilder.build();
        
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
