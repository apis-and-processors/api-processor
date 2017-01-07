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

package com.github.api.processor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.github.api.processor.annotations.Api;
import com.github.api.processor.cache.ApiProcessorCache;
import com.github.api.processor.config.ApiRegistrationModule;
import com.github.api.processor.config.HandlerRegistrationModule;
import com.github.api.processor.config.StandAloneModules;
import com.github.api.processor.handlers.AbstractErrorHandler;
import com.github.api.processor.handlers.AbstractExecutionHandler;
import com.github.api.processor.handlers.AbstractFallbackHandler;
import com.github.api.processor.handlers.AbstractResponseHandler;
import com.github.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.github.api.processor.utils.ApiProcessorUtils;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.Properties;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author github.
 */
public class ApiProcessor {
    
    private final Injector injector;
    
    private ApiProcessor(Injector injector) {
        this.injector = injector;
    }
    
    public <T> T get(Class<T> clazz) {
        return injector.getInstance(clazz);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
    
        private final Logger logger = Logger.getLogger(ApiProcessor.class.getName());
        private final Set<Class> apis = Sets.newHashSet();
        private final Set<Module> modules = Sets.newHashSet();
        private final Properties properties = new Properties();
        
        private boolean scanClasspath = false;
        
        private Class<?> executionContext;

        private Class<? extends AbstractExecutionHandler> executionHandler;
        private Class<? extends AbstractErrorHandler> errorHandler;
        private Class<? extends AbstractFallbackHandler> fallbackHandler;
        private Class<? extends AbstractResponseHandler> responseHandler;
        
        public Builder api(Class clazz) {
            this.apis.add(clazz);
            return this;
        }
              
        /**
         * Add module to Guice injection.
         * 
         * @param module the module to add.
         * @return this Builder.
         */
        public Builder module(Module module) {
            checkNotNull(module, "module cannot be null");
            modules.add(module);
            return this;
        }
        
        /**
         * Add properties to be used within various contexts within ApiProcessor.
         * 
         * @param properties the properties to add.
         * @return this Builder.
         */
        public Builder properties(Properties properties) {
            this.properties.putAll(checkNotNull(properties, "properties cannot be null"));
            return this;
        }
        
        /**
         * Add a single property to be used within various contexts within ApiProcessor.
         * 
         * @param key the key of property
         * @param value the value of property
         * @return this Builder.
         */
        public Builder properties(String key, String value) {
            checkNotNull(key, "key cannot be null");
            checkNotNull(value, "value cannot be null");
            this.properties.put(key, value);
            return this;
        }
        
        /**
         * Whether to scan classpath for Interfaces annotated with @Api. Defaults to false.
         * 
         * @return this Builder.
         */
        public Builder scanClasspath() {
            this.scanClasspath = true;
            return this;
        }
        
        /**
         * Set the global ExecutionContext. Optional and defaults to null.
         * 
         * @param executionContext global ExecutionContext.
         * @return this Builder.
         */
        public Builder executionContext(Class<?> executionContext) {
            this.executionContext = checkNotNull(executionContext, "executionContext cannot be null");
            return this;
        }
        
        /**
         * Set the global ExecutionHandler. Optional and defaults to null.
         * 
         * @param executionHandler global ExecutionHandler.
         * @return this Builder.
         */
        public Builder executionHandler(Class<? extends AbstractExecutionHandler> executionHandler) {
            this.executionHandler = checkNotNull(executionHandler, "executionHandler cannot be null");
            return this;
        }
        
        /**
         * Set the global ErrorHandler. Optional and defaults to null.
         * 
         * @param errorHandler global ErrorHandler.
         * @return this Builder.
         */
        public Builder errorHandler(Class<? extends AbstractErrorHandler> errorHandler) {
            this.errorHandler = checkNotNull(errorHandler, "errorHandler cannot be null");
            return this;
        }
           
        /**
         * Set the global FallbackHandler. Optional and defaults to null.
         * 
         * @param fallbackHandler global FallbackHandler.
         * @return this Builder.
         */
        public Builder fallbackHandler(Class<? extends AbstractFallbackHandler> fallbackHandler) {
            this.fallbackHandler = checkNotNull(fallbackHandler, "fallbackHandler cannot be null");
            return this;
        }
        
        /**
         * Set the global ResponseHandler. Optional and defaults to null.
         * 
         * @param responseHandler global ResponseHandler.
         * @return this Builder.
         */
        public Builder responseHandler(Class<? extends AbstractResponseHandler> responseHandler) {
            this.responseHandler = checkNotNull(responseHandler, "responseHandler cannot be null");
            return this;
        }
        
        /**
         * Build an ApiProcessor from passed build parameters.
         * 
         * @return newly created ApiProcessor.
         */
        public ApiProcessor build() {
            
            // 1.) Create parent injector from stand alone modules.
            StandAloneModules sam = new StandAloneModules(properties);
            HandlerRegistrationModule hrm = new HandlerRegistrationModule(executionContext, executionHandler, errorHandler, fallbackHandler, responseHandler);
            Injector parentInjector = Guice.createInjector(sam, hrm);

            // 2.) Gather all Api's passed in and on classpath.
            Set<Class> builtApis = Sets.newHashSet(apis);
            if (this.scanClasspath) {
                ApiProcessorUtils processorUtils = parentInjector.getInstance(ApiProcessorUtils.class);
                builtApis.addAll(processorUtils.findClassesAnnotatedWith(Api.class));
            }
                        
            checkArgument(builtApis.size() > 0, "must have at least 1 api to initialize processor");
            builtApis.stream().forEach(entry -> {
                logger.log(Level.INFO, "Found Api @ {0}", entry.getName());
            });
            
            // 3.) Create child injector and build ApiProcessor.
            AbstractRuntimeInvocationHandler apiProcessorInvocationHandler = parentInjector.getInstance(AbstractRuntimeInvocationHandler.class);
            ApiProcessorCache apiProcessorCache = parentInjector.getInstance(ApiProcessorCache.class);
            modules.add(new ApiRegistrationModule(builtApis, apiProcessorInvocationHandler, apiProcessorCache));
            Injector childInjector = parentInjector.createChildInjector(modules);
            return new ApiProcessor(childInjector);
        }
    }
}
