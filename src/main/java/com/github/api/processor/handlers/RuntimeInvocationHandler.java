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

package com.github.api.processor.handlers;

import com.github.api.processor.ApiProcessorConstants;
import com.github.api.processor.ApiProcessorProperties;
import com.github.api.processor.annotations.Delegate;
import com.github.api.processor.wrappers.ResponseWrapper;
import com.github.api.processor.instance.InvocationInstance;
import com.github.api.processor.cache.ApiProcessorCache;
import com.github.api.processor.exceptions.TypeMismatchException;
import com.github.api.processor.utils.ApiProcessorUtils;
import com.github.api.processor.wrappers.ErrorWrapper;
import com.github.api.processor.wrappers.FallbackWrapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Injector;
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
 * @author github.
 */
@Singleton
public class RuntimeInvocationHandler extends AbstractRuntimeInvocationHandler {
    
    private static final Logger LOGGER = Logger.getLogger(RuntimeInvocationHandler.class.getName());

    private static final String DELEGATE_MESSAGE = "Delegate method returning instance of {0}";
    private static final String RE_INJECT_MEMBERS = "Re-injecting memebers as executionContext has been changed";
    private static final String RETRY_ATTEMPT_MESSAGE = "Invocation attempt failed due to: {0}";
    private static final String RETRY_FAILED_MESSAGE = "Invocation failed due to: {0}";
    private static final String RETRY_RUN_MESSAGE = "Invocation attempt {0} on {1}";
    
    @Inject
    Injector injector;
        
    @Inject
    ApiProcessorCache processorCache;
    
    @Inject
    ApiProcessorUtils processorUtils;
    
    @Inject(optional = true)
    @Nullable
    AbstractRequestHandler abstractRequestHandler;
        
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
        final InvocationInstance invocationInstance = processorCache.invocationInstanceFrom(method, args);
        
        // 2.) If method is a Delegate then return an instance of its Api/Interface
        if (invocationInstance.methodAnnotation(Delegate.class) != null) {
            Class proxyType = invocationInstance.returnType().getRawType();
            LOGGER.log(Level.INFO, DELEGATE_MESSAGE, proxyType);
            return processorCache.proxyFrom(proxyType, this);
        }
        
        // 3.) Initialize handlers, if present, for runtime execution
        final AbstractRequestHandler runtimeRequestHandler = (invocationInstance.requestHandler() != null) 
                ? invocationInstance.requestHandler()
                : abstractRequestHandler;
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
                
        // 4.) 2 things are happening below: we are optionally executing a RequestHandler and 
        //     generating, not optional, an executionContext. The RequestHandler takes in an 
        //     executionContext which is why we have to build/generate it as part of its 
        //     invocation. If we don't execute a RequestHandler we are still required to 
        //     build/generate an executionContext.
        Object executionContext;
        Class genericExecutionType = processorUtils.getGenericClassTypes(runtimeExecutionHandler.getClass())[0];
        if (runtimeRequestHandler != null) {
            Class genericRequestType = processorUtils.getGenericClassTypes(runtimeRequestHandler.getClass())[0];
            executionContext = getInstance(genericRequestType);
            
            // 4.1) Attempt to inject members for the recently created 'executionContext'. As it is 
            //      possible that the returned 'executionContext' from the requestHandler is different
            //      than the one we passed in, we check their hashCodes to determine difference and if 
            //      so then re-inject members below at the appropriate time.
            int currentHashCode = executionContext.hashCode();
            executionContext = runtimeRequestHandler.apply(executionContext);
            
            if (executionContext != null) {
                if (genericExecutionType.equals(executionContext.getClass())) {
                    // 4.2) Re-inject members if hashCodes are different.
                    if (executionContext.hashCode() != currentHashCode) {
                        LOGGER.log(Level.WARNING, RE_INJECT_MEMBERS);
                        injector.injectMembers(executionContext);
                    }                    
                } else {
                    throw new TypeMismatchException("RequestHandler (" 
                            + runtimeRequestHandler.getClass().getCanonicalName() + ") returned type '" 
                            + executionContext.getClass().getCanonicalName() + "' while ExecutionHandler (" 
                            + runtimeExecutionHandler.getClass().getCanonicalName() + ") expects type '" 
                            + genericExecutionType.getCanonicalName() + "'. These MUST be the same!!!");
                }
            } else {
                throw new NullPointerException("Returned value from " 
                        + runtimeRequestHandler.getClass().getCanonicalName() 
                        + " cannot be null");
            }
        } else {
            executionContext = getInstance(genericExecutionType);
        }
       
        // 5.) Pass InvocationInstance to ExecutionHandler for runtime execution.
        final AtomicReference<Object> responseReference = new AtomicReference();
        Throwable invocationException = null;
        try {
            
            // set context for execution
            invocationInstance.context(executionContext);

            String retryCount = properties.get(ApiProcessorConstants.RETRY_COUNT, ApiProcessorConstants.RETRY_COUNT_DEFAULT);
            String retryDelayStart = properties.get(ApiProcessorConstants.RETRY_DELAY_START, ApiProcessorConstants.RETRY_DELAY_START_DEFAULT);

            RetryPolicy retryPolicy = new RetryPolicy()
                    .withDelay(Long.valueOf(retryDelayStart), TimeUnit.MILLISECONDS)
                    .withMaxRetries(Integer.valueOf(retryCount));
            
            Failsafe.with(retryPolicy)
                    .onFailedAttempt(attempt -> LOGGER.log(Level.WARNING, RETRY_ATTEMPT_MESSAGE, attempt.getMessage()))
                    .onFailure(failure -> LOGGER.log(Level.SEVERE, RETRY_FAILED_MESSAGE, failure.getMessage()))
                    .run((ctx) -> { 
                        Object [] loggerParams = {ctx.getExecutions() + 1, invocationInstance.toString()};
                        LOGGER.log(Level.INFO, RETRY_RUN_MESSAGE, loggerParams);
                        Object responseObject = runtimeExecutionHandler.apply(invocationInstance);
                        responseReference.set(responseObject); 
                    });
                        
        } catch (Exception e) {
            invocationException = e;
        }
        
        // 6.) Optionally, if exception was found during execution then pass to errorHandler for marshalling
        if (invocationException != null && runtimeErrorHandler != null) {
            try {
                Class genericErrorType = processorUtils.getGenericClassTypes(runtimeErrorHandler.getClass())[0];
                if (genericErrorType.equals(executionContext.getClass())) {
                    ErrorWrapper<?> errorWrapper = ErrorWrapper.newInstance(invocationInstance, invocationException);
                    Throwable possibleThrowable = (Throwable) runtimeErrorHandler.apply(errorWrapper);
                    if (possibleThrowable != null) {
                        invocationException = possibleThrowable;
                    }                         
                } else {                    
                    throw new TypeMismatchException("ErrorHandler (" 
                            + runtimeErrorHandler.getClass().getCanonicalName() + ") takes type '" 
                            + genericErrorType.getCanonicalName() + "' but is required to accept type '" 
                            + genericExecutionType.getCanonicalName() + "'. These MUST be the same!!!");
                }
            } catch (Exception propagatedException) {
                invocationException = propagatedException;
            }
        }
        
        // 7.) Optionally, if exception was not previously handled (perhaps re-thrown as something else), then pass to fallbackHandler
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
        
        // 8.) Optionally, we can marshall the response to some other object 
        if (!fallbackInvoked && runtimeResponseHandler != null) {
            ResponseWrapper<?> responseWrapper = ResponseWrapper.newInstance(responseReference.get(), invocationInstance.context(), invocationInstance.returnType());
            Object responseObject = runtimeResponseHandler.apply(responseWrapper);
            responseReference.set(responseObject);
        } 
        
        return responseReference.get();
    }
    
    private Object getInstance(Class clazz) {
        Object instance;
        try {
            instance = injector.getInstance(clazz);
        } catch (Exception e) {
            instance = processorUtils.newClassInstance(clazz);
            injector.injectMembers(instance);
        }
        return instance;
    }
}
