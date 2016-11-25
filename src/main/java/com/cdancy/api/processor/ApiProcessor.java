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

package com.cdancy.api.processor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cdancy.api.processor.annotations.Api;
import com.cdancy.api.processor.config.ApiRegistrationModule;
import com.cdancy.api.processor.config.HandlerRegistrationModule;
import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.utils.ProcessorUtils;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cdancy.
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
        private boolean scanClasspath = false;
        
        private Class<? extends AbstractExecutionHandler> executionHandler;
        private Class<? extends AbstractErrorHandler> errorHandler;
        private Class<? extends AbstractFallbackHandler> fallbackHandler;
        private Class<? extends AbstractResponseHandler> responseHandler;
        
        public Builder api(Class clazz) {
            this.apis.add(clazz);
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
            
            // 1.) Gather all Api's passed in and on classpath.
            Set<Class> builtApis = Sets.newHashSet(apis);
            if (this.scanClasspath) {
                builtApis.addAll(ProcessorUtils.findClassesAnnotatedWith(Api.class));
            }
            
            checkArgument(builtApis.size() > 0, "must have at least 1 api to initialize processor");
            builtApis.stream().forEach(entry -> {
                logger.log(Level.INFO, "Found Api @ {0}", entry.getName());
            });

            // 2.) Create injector from modules and build ApiProcessor.
            HandlerRegistrationModule hrm = new HandlerRegistrationModule(executionHandler, errorHandler, fallbackHandler, responseHandler);
            final Injector handlerInjector = Guice.createInjector(hrm);
            AbstractRuntimeInvocationHandler apiProcessorInvocationHandler = handlerInjector.getInstance(AbstractRuntimeInvocationHandler.class);
            final Injector apiInjector = handlerInjector.createChildInjector(new ApiRegistrationModule(builtApis, apiProcessorInvocationHandler));
            return new ApiProcessor(apiInjector);
        }
    }
}
