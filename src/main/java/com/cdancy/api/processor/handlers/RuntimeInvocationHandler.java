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

package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.ApiProcessorConstants;
import com.cdancy.api.processor.ApiProcessorProperties;
import com.cdancy.api.processor.wrappers.ResponseWrapper;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.cache.ApiProcessorCache;
import com.cdancy.api.processor.wrappers.ErrorWrapper;
import com.cdancy.api.processor.wrappers.FallbackWrapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 *
 * @author cdancy.
 */
@Singleton
public class RuntimeInvocationHandler extends AbstractRuntimeInvocationHandler {
    
    private static final Logger LOGGER = Logger.getLogger(RuntimeInvocationHandler.class.getName());

    @Inject
    ApiProcessorCache processorCache;
        
    @Inject
    AbstractExecutionHandler abstractExecutionHandler;
    
    @Inject(optional = true)
    @Nullable
    AbstractErrorHandler abstractErrorHandler;
        
    @Inject(optional = true)
    @Nullable
    AbstractFallbackHandler abstractFallbackHandler;
 
    @Inject(optional = true)
    @Nullable
    AbstractResponseHandler abstractResponseHandler;
        
    @Inject
    private ApiProcessorProperties properties;
        
    @Override
    protected Object handleInvocation(Object source, Method method, Object[] args) {
                        
        // 1.) Get/Build InvocationInstance from cache.
        final InvocationInstance<?> invocationInstance = processorCache.invocationInstanceFrom(method, args);
        
        // 2.) Initialize handlers, if present, for runtime execution
        final AbstractExecutionHandler runtimeExecutionHandler = (invocationInstance.executionHandler() != null) 
                ? invocationInstance.executionHandler() 
                : abstractExecutionHandler;
        final AbstractErrorHandler runtimeErrorHandler = (invocationInstance.errorHandler() != null) 
                ? invocationInstance.errorHandler() 
                : abstractErrorHandler;
        final AbstractFallbackHandler runtimeFallbackHandler = (invocationInstance.fallbackHandler() != null) 
                ? invocationInstance.fallbackHandler() 
                : abstractFallbackHandler;
        final AbstractResponseHandler runtimeResponseHandler = (invocationInstance.responseHandler() != null) 
                ? invocationInstance.responseHandler() 
                : abstractResponseHandler;
        
        
        // 3.) Pass InvocationInstance to ExecutionHandler for runtime execution.
        final AtomicReference responseReference = new AtomicReference();
        Throwable invocationException = null;
        try {
            
            String retryCount = properties.get(ApiProcessorConstants.RETRY_COUNT, "0");
            String retryDelayStart = properties.get(ApiProcessorConstants.RETRY_DELAY_START, "5000");

            RetryPolicy retryPolicy = new RetryPolicy()
                    .withDelay(Long.valueOf(retryDelayStart), TimeUnit.MILLISECONDS)
                    .withMaxRetries(Integer.valueOf(retryCount));
            
            Failsafe.with(retryPolicy)
                    .onFailedAttempt(attempt -> LOGGER.log(Level.WARNING, "Invocation attempt failed due to: {0}", attempt.getMessage()))
                    .onFailure(failure -> LOGGER.log(Level.SEVERE, "Invocation failed due to: {0}", failure.getMessage()))
                    .run((ctx) -> { 
                        LOGGER.log(Level.INFO, "Invocation attempt {0} on " + invocationInstance, ctx.getExecutions() + 1);
                        Object responseObject = runtimeExecutionHandler.apply(invocationInstance);
                        responseReference.set(responseObject); 
                    });
                        
        } catch (Exception e) {
            invocationException = e;
        }
        
        // 4.) Optionally, if exception was found during execution then pass to errorHandler for marshalling
        if (invocationException != null && runtimeErrorHandler != null) {
            try {
                ErrorWrapper errorWrapper = ErrorWrapper.newInstance(invocationInstance, invocationException);
                Throwable possibleThrowable = (Throwable) runtimeErrorHandler.apply(errorWrapper);
                if (possibleThrowable != null) {
                    invocationException = possibleThrowable;
                }
            } catch (Exception propagatedException) {
                invocationException = propagatedException;
            }
        }
        
        // 5.) Optionally, if exception was not previously handled (perhaps re-thrown as something else), then pass to fallbackHandler
        boolean fallbackInvoked = false;
        if (invocationException != null) {
            if (runtimeFallbackHandler != null) {
                try {
                    FallbackWrapper fallbackWrapper = FallbackWrapper.newInstance(invocationInstance.returnType(), invocationException);
                    Object responseObject = runtimeFallbackHandler.apply(fallbackWrapper);
                    responseReference.set(responseObject);
                    fallbackInvoked = true;
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            } else {
                throw Throwables.propagate(invocationException);
            }
        } 
        
        // 6.) Optionally, we can marshall the response to some other object 
        if (!fallbackInvoked && runtimeResponseHandler != null) {
            ResponseWrapper<?> responseWrapper = ResponseWrapper.newInstance(responseReference.get(), invocationInstance.returnType());
            Object responseObject = runtimeResponseHandler.apply(responseWrapper);
            responseReference.set(responseObject);
        } 
        
        return responseReference.get();
    }
}
