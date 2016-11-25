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

package com.cdancy.api.processor.config;

import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.utils.ProcessorUtils;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import java.util.Set;

/**
 *
 * @author cdancy.
 */
public class ApiRegistrationModule extends AbstractModule {

    private final Set<Class> apis;
    private final AbstractRuntimeInvocationHandler abstractRuntimeInvocationHandler;
            
    public ApiRegistrationModule(Set<Class> apis, AbstractRuntimeInvocationHandler abstractRuntimeInvocationHandler) {
        this.apis = ImmutableSet.copyOf(apis);
        this.abstractRuntimeInvocationHandler = abstractRuntimeInvocationHandler;
    }
        
    @Override 
    protected void configure() {
        apis.stream().forEach(entry -> {
            bind(entry).toInstance(ProcessorUtils.newTypeFrom(entry, abstractRuntimeInvocationHandler));
        });
    }
}