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

import com.cdancy.api.processor.ApiProcessorProperties;
import com.cdancy.api.processor.cache.ApiProcessorCache;
import com.cdancy.api.processor.utils.ApiProcessorUtils;
import com.google.inject.AbstractModule;
import java.util.Properties;

/**
 *
 * @author cdancy.
 */
public class StandAloneModules extends AbstractModule {
            
    private final Properties properties;

    public StandAloneModules(Properties properties) {
        this.properties = properties;
    }
        
    @Override 
    protected void configure() {
        ApiProcessorProperties apiProcessorProperties = new ApiProcessorProperties(properties);
        ApiProcessorCache apiProcessorCache = new ApiProcessorCache(apiProcessorProperties);
        ApiProcessorUtils apiProcessorUtils = new ApiProcessorUtils();
        
        bind(ApiProcessorProperties.class).toInstance(apiProcessorProperties);
        bind(ApiProcessorCache.class).toInstance(apiProcessorCache);
        bind(ApiProcessorUtils.class).toInstance(apiProcessorUtils);
    }
}