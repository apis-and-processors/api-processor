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

import com.cdancy.api.processor.cache.ProcessorCache;
import com.cdancy.api.processor.utils.ProcessorUtils;
import com.google.inject.AbstractModule;

/**
 *
 * @author cdancy.
 */
public class StandAloneModules extends AbstractModule {
            
    private final ProcessorCache processorCache;
    private final ProcessorUtils processorUtils;

    public StandAloneModules(ProcessorCache processorCache, ProcessorUtils processorUtils) {
        this.processorCache = processorCache;
        this.processorUtils = processorUtils;
    }
        
    @Override 
    protected void configure() {
        bind(ProcessorCache.class).toInstance(processorCache);
        bind(ProcessorUtils.class).toInstance(processorUtils);
    }
}