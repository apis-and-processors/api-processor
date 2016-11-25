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

import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.DefaultExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.handlers.DefaultRuntimeInvocationHandler;
import com.google.inject.AbstractModule;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy.
 */
public class HandlerRegistrationModule extends AbstractModule {
        
    private final Class executionHandler;
    private final Class responseHandler;
    private final Class errorHandler;
    private final Class fallbackHandler;
    
    /**
     * Create HandlerRegistrationModule from the potentially non-null classes.
     * 
     * @param executionHandler default ExecutionHandler to set.
     * @param errorHandler default ErrorHandler to set.
     * @param fallbackHandler default FallbackHandler to set.
     * @param responseHandler default ResponseHandler to set.
     */
    public HandlerRegistrationModule(@Nullable Class<? extends AbstractExecutionHandler> executionHandler, 
            @Nullable Class<? extends AbstractErrorHandler> errorHandler,
            @Nullable Class<? extends AbstractFallbackHandler> fallbackHandler,
            @Nullable Class<? extends AbstractResponseHandler> responseHandler) {
        this.executionHandler = executionHandler;
        this.errorHandler = errorHandler;
        this.fallbackHandler = fallbackHandler;
        this.responseHandler = responseHandler;
    }
            
    @Override 
    protected void configure() {

        bind(AbstractRuntimeInvocationHandler.class).to(DefaultRuntimeInvocationHandler.class);

        Class defaultExecutionHandler = (executionHandler != null) 
                ? executionHandler 
                : DefaultExecutionHandler.class;
        bind(AbstractExecutionHandler.class).to(defaultExecutionHandler);
        
        if (responseHandler != null) {
            bind(AbstractResponseHandler.class).to(responseHandler);
        }
        if (errorHandler != null) {
            bind(AbstractErrorHandler.class).to(errorHandler);
        }
        if (fallbackHandler != null) {
            bind(AbstractFallbackHandler.class).to(fallbackHandler);
        }
    }
}