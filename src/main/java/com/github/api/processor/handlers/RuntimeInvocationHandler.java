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
import com.github.api.processor.utils.Primitive;
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
        
        // 4.) Check that Types passed between handlers are sane and not mismatched. 
        //     Throws RuntimeException if something does not match correctly.
        checkTypeConsistency(runtimeRequestHandler,
                runtimeExecutionHandler,
                runtimeErrorHandler,
                runtimeFallbackHandler,
                runtimeResponseHandler,
                invocationInstance.returnType().getRawType());
                
        // 4.) 2 things are happening below: we are optionally executing a RequestHandler and 
        //     generating, not optional, an executionContext. The RequestHandler takes in an 
        //     executionContext which is why we have to build/generate it as part of its 
        //     invocation. If we don't execute a RequestHandler we are still required to 
        //     build/generate an executionContext.
        Object executionContext;
        Class genericExecutionType = processorUtils.getGenericClassTypes(runtimeExecutionHandler.getClass())[0];
        if (runtimeRequestHandler != null) {
            Class[] genericRequestTypes = processorUtils.getGenericClassTypes(runtimeRequestHandler.getClass());
            executionContext = getInstance(genericRequestTypes[0]);
            
            // 4.1) Because execution of the RequestHandler is allowed to return a 
            //      different type of Object than what potentially went in, we need 
            //      to check if things ARE different and if so inject potential members.
            Object possibleyNewObject = runtimeRequestHandler.apply(executionContext);            
            if (possibleyNewObject != null) {
                
                // 4.2) Re-inject members if hashCodes are different.
                if (executionContext.hashCode() != possibleyNewObject.hashCode()) {                            
                    injector.injectMembers(possibleyNewObject);
                    executionContext = possibleyNewObject;
                } 
            } else {
                
                // 4.3) A returned NULL executionContext from the RequestHandler 
                //      is only allowed if the ExecutionHandler has an input of 
                //      type java.lang.Void.
                if (!genericExecutionType.equals(Primitive.VOID.getRawClass())) {
                    throw new NullPointerException("RequestHandler (" 
                            + runtimeRequestHandler.getClass().getCanonicalName() + ") returned NULL while ExecutionHandler '" 
                            + runtimeExecutionHandler.getClass().getCanonicalName() + "' expects an input of type '" 
                            + genericExecutionType.getCanonicalName() + "'. This is only allowed if the input is of type '" 
                            + Primitive.VOID.getRawClass() + "'");
                }
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
                ErrorWrapper<?> errorWrapper = ErrorWrapper.newInstance(invocationInstance, invocationException);
                Throwable possibleThrowable = (Throwable) runtimeErrorHandler.apply(errorWrapper);
                if (possibleThrowable != null) {
                    invocationException = possibleThrowable;
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
            ResponseWrapper<?, ?> responseWrapper = ResponseWrapper.newInstance(responseReference.get(), invocationInstance.context(), invocationInstance.returnType());
            Object responseObject = runtimeResponseHandler.apply(responseWrapper);
            responseReference.set(responseObject);
        } 
        
        return responseReference.get();
    }
    
    private void checkTypeConsistency(@Nullable AbstractRequestHandler runtimeRequestHandler,
            AbstractExecutionHandler runtimeExecutionHandler,
            @Nullable AbstractErrorHandler runtimeErrorHandler,
            @Nullable AbstractFallbackHandler runtimeFallbackHandler,
            @Nullable AbstractResponseHandler runtimeResponseHandler,
            Class expectedReturnType) {
        
        // The only thing guaranteed to be non-null is the ExecutionHandler 
        // which is why we init it here.
        Class[] genericExecutionTypes = processorUtils.getGenericClassTypes(runtimeExecutionHandler.getClass());
        Class comparisonSafeReturnType = processorUtils.potentialPrimitiveToClass(expectedReturnType);
        
        
        // 1.) Check RequestHandler, if applicable, for initial Type as its output  
        //     must match the input of ExecutionHandler.
        if (runtimeRequestHandler != null) {
            Class genericRequestOutputType = processorUtils.getGenericClassTypes(runtimeRequestHandler.getClass())[1];
            if (!genericRequestOutputType.equals(genericExecutionTypes[0])) {
                throw new TypeMismatchException("RequestHandler (" 
                        + runtimeRequestHandler.getClass().getCanonicalName() + ") has an output of type '" 
                        + genericRequestOutputType.getCanonicalName() + "' while ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") expects an input of type '" 
                        + genericExecutionTypes[0].getCanonicalName() + "'. These MUST be the same!!!");
            }
        }
        
        
        // 2.) Check the ErrorHandler input, if applicable, as it must match the 
        //     the input, that is the context, to the ExecutionHandler.
        if (runtimeErrorHandler != null) {
            Class genericErrorInputType = processorUtils.getGenericClassTypes(runtimeErrorHandler.getClass())[0];
            if (!genericErrorInputType.equals(genericExecutionTypes[0])) {
                throw new TypeMismatchException("ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") has an input of type '" 
                        + genericExecutionTypes[0].getCanonicalName() + "' while ErrorHandler (" 
                        + runtimeErrorHandler.getClass().getCanonicalName() + ") expects an input of type '" 
                        + genericErrorInputType.getCanonicalName() + "'. These MUST be the same!!!");
            }
        }

            
        // 3.) Check the FallbackHandler output, if applicable, as it must match 
        //     the expected returnType.
        if (runtimeFallbackHandler != null) {
            Class genericFallbackOutputType = processorUtils.getGenericClassTypes(runtimeFallbackHandler.getClass())[0];
            if (!genericFallbackOutputType.equals(comparisonSafeReturnType)) {
                throw new TypeMismatchException("FallbackHandler (" 
                        + runtimeFallbackHandler.getClass().getCanonicalName() + ") has an output of type '" 
                        + genericFallbackOutputType.getCanonicalName() + "' while expected returnType is of type '" 
                        + comparisonSafeReturnType.getCanonicalName() + "'. These MUST be the same!!!");
            } 
        }

        
        // 4.) Check the ResponseHandler input, if applicable, as it must match  
        //     the ExecutionHandler output. 
        //
        //     Also check the ResponseHandler output as it must match the 
        //     expected returnType. 
        if (runtimeResponseHandler != null) {
            Class[] genericResponseTypes = processorUtils.getGenericClassTypes(runtimeResponseHandler.getClass());
            if (!genericResponseTypes[0].equals(genericExecutionTypes[1])) {
                throw new TypeMismatchException("ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") has an output of type '" 
                        + genericExecutionTypes[1].getCanonicalName() + "' while ResponseHandler (" 
                        + runtimeResponseHandler.getClass().getCanonicalName() + ") expects an input of type '" 
                        + genericResponseTypes[0].getCanonicalName() + "'. These MUST be the same!!!");
            } else if (!genericResponseTypes[1].equals(comparisonSafeReturnType)) {
                throw new TypeMismatchException("ResponseHandler (" 
                        + runtimeResponseHandler.getClass().getCanonicalName() + ") has an output of type '" 
                        + genericResponseTypes[1].getCanonicalName() + "' while expected returnType is of type '" 
                        + comparisonSafeReturnType.getCanonicalName() + "'. These MUST be the same!!!");
            } 
        } else {
            
            // 5.) If no ResponseHandler was registered then the ExecutionHandler
            //     is required to return the correct returnType.
            if (runtimeResponseHandler == null && !comparisonSafeReturnType.equals(genericExecutionTypes[1])) { 
                throw new TypeMismatchException("ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") has an output of type '" 
                        + genericExecutionTypes[1].getCanonicalName() + "' while expected returnType is of type (" 
                        + comparisonSafeReturnType.getCanonicalName() + "'. These MUST be the same!!!");
            }
        }
    }
    
    private Object getInstance(Class clazz) {
        Object instance;
        try {
            return (clazz.equals(Primitive.VOID.getRawClass())) ? null : injector.getInstance(clazz);                
        } catch (Exception e) {
            instance = processorUtils.newClassInstance(clazz);
            injector.injectMembers(instance);
        }
        return instance;
    }
}
