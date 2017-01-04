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

package com.github.api.processor.config;

import com.github.api.processor.cache.ApiProcessorCache;
import com.github.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import java.util.Set;

/**
 *
 * @author github.
 */
public class ApiRegistrationModule extends AbstractModule {

    private final Set<Class> apis;
    private final AbstractRuntimeInvocationHandler abstractRuntimeInvocationHandler;
    private final ApiProcessorCache processorCache;

    /**
     * Create ApiRegistrationModule from passed parameters.
     * 
     * @param apis the Set of Classes to register as API's.
     * @param abstractRuntimeInvocationHandler the default handler each API will invoke on method execution.
     * @param processorCache cache to create new proxy instances from.
     */
    public ApiRegistrationModule(Set<Class> apis, 
            AbstractRuntimeInvocationHandler abstractRuntimeInvocationHandler, 
            ApiProcessorCache processorCache) {
        this.apis = ImmutableSet.copyOf(apis);
        this.abstractRuntimeInvocationHandler = abstractRuntimeInvocationHandler;
        this.processorCache = processorCache;
    }
        
    @Override 
    protected void configure() {
        apis.stream().forEach(entry -> {
            bind(entry).toInstance(processorCache.proxyFrom(entry, abstractRuntimeInvocationHandler));
        });
    }
}