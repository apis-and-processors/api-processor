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
import com.google.common.collect.Maps;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 *
 * @author cdancy.
 * @param <T>
 */
public class ClassInstance<T> implements ProcessorHandles {
    
    private final Class<T> clazz;
    private final ImmutableMap<String, ImmutableList<Annotation>> annotations;

    private final Class<? extends AbstractExecutionHandler> executionHandler;
    private final Class<? extends AbstractErrorHandler> errorHandler;
    private final Class<? extends AbstractFallbackHandler> fallbackHandler;
    private final Class<? extends AbstractResponseHandler> responseHandler;

    /**
     * Create ClassInstance from passed class.
     * 
     * @param clazz Class definition.
     */
    public ClassInstance(Class<T> clazz) {
        this.clazz = clazz;
        
        Class localExecutionHandler = null;
        Class localErrorHandler = null;
        Class localFallbackHandler = null;
        Class localResponseHandler = null;
        
        this.annotations = buildClassAnnotationMap(clazz);
        
        /**
         * We always use the first annotation found below as that is the one closest 
         * to the method definition.
         */
        ImmutableList<Annotation> possibleAnnotationList = this.annotations.get(ExecutionHandler.class.getName());
        Annotation possibleAnnotation = (possibleAnnotationList != null) ? possibleAnnotationList.get(0) : null;
        if (possibleAnnotation != null) {
            ExecutionHandler anno = (ExecutionHandler)possibleAnnotation;
            localExecutionHandler = anno.value();
        }
        
        possibleAnnotationList = this.annotations.get(ErrorHandler.class.getName());
        possibleAnnotation = (possibleAnnotationList != null) ? possibleAnnotationList.get(0) : null;
        if (possibleAnnotation != null) {
            ErrorHandler anno = (ErrorHandler)possibleAnnotation;
            localErrorHandler = anno.value();
        }
        
        possibleAnnotationList = this.annotations.get(FallbackHandler.class.getName());
        possibleAnnotation = (possibleAnnotationList != null) ? possibleAnnotationList.get(0) : null;
        if (possibleAnnotation != null) {
            FallbackHandler anno = (FallbackHandler)possibleAnnotation;
            localFallbackHandler = anno.value();
        }
        
        possibleAnnotationList = this.annotations.get(ResponseHandler.class.getName());
        possibleAnnotation = (possibleAnnotationList != null) ? possibleAnnotationList.get(0) : null;
        if (possibleAnnotation != null) {
            ResponseHandler anno = (ResponseHandler)possibleAnnotation;
            localResponseHandler = anno.value();
        }
        
        this.executionHandler = localExecutionHandler;
        this.errorHandler = localErrorHandler;
        this.fallbackHandler = localFallbackHandler;
        this.responseHandler = localResponseHandler;
    }
    
    private ImmutableMap<String, ImmutableList<Annotation>> buildClassAnnotationMap(Class clazz) {
        
        Map<String, ImmutableList.Builder<Annotation>> clazzAnnotationMap = Maps.newHashMap();
        Class currentClass = clazz;
        while (currentClass != null) {
            for (Annotation clazzAnnotation : currentClass.getAnnotations()) {
                String annoName = (clazzAnnotation.annotationType().getName()).intern();
                ImmutableList.Builder<Annotation> possibleList = clazzAnnotationMap.get(annoName);
                if (possibleList == null) {
                    possibleList = ImmutableList.builder();
                    clazzAnnotationMap.put(annoName, possibleList);
                }
                possibleList.add(clazzAnnotation);
            }
            
            // always, and really only, need to get the 0th element as we are only dealing with interfaces
            // and the interfaces themselves can only extend a single class (which is the thing we are after).
            currentClass = (currentClass.getInterfaces().length > 0) ? currentClass.getInterfaces()[0] : null;
        }
        
        ImmutableMap.Builder<String, ImmutableList<Annotation>> mapBuilder = ImmutableMap.builder();
        clazzAnnotationMap.entrySet().forEach((entry) -> {
            mapBuilder.put(entry.getKey(), entry.getValue().build());
        });
        
        return mapBuilder.build();
    }
    
    public Class<T> clazz() {
        return clazz;
    }
    
    public ImmutableMap<String, ImmutableList<Annotation>> annotations() {
        return annotations;
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
