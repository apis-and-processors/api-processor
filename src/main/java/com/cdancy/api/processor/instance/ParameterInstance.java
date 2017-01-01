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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.lang.annotation.Annotation;

/**
 *
 * @author cdancy.
 * @param <T>
 */
public class ParameterInstance<T> {
    
    private final Class clazz;
    private final ImmutableMap<String, Annotation> annotations;
    
    // value is typically set AFTER object instantiation
    private T value = null;
    
    /**
     * Create ParameterInstance from passed args.
     * 
     * @param clazz Class these parameters belong to.
     * @param annotations Annotations placed on each parameter.
     */
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
    
    public void setValue(T value) {
        this.value = value;
    }
        
    public T getValue() {
        return value;
    }
}
