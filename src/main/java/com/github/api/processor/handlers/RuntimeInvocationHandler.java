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
import com.github.api.processor.exceptions.CheckTimeTypeMismatchException;
import com.github.api.processor.exceptions.NullNotAllowedException;
import com.github.api.processor.exceptions.ProcessTimeTypeMismatchException;
import com.github.api.processor.utils.ApiProcessorUtils;
import com.github.api.processor.utils.Constants;
import com.github.api.processor.utils.Pair;
import com.github.api.processor.wrappers.ErrorWrapper;
import com.github.api.processor.wrappers.FallbackWrapper;
import com.github.type.utils.ClassType;
import com.github.type.utils.PrimitiveTypes;
import com.github.type.utils.TypeUtils;
import com.github.type.utils.exceptions.TypeMismatchException;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
    
    private static final Cache<String, Object> RUNTIME_METADATA = CacheBuilder.newBuilder().build();
    
    private static final Logger LOGGER = Logger.getLogger(RuntimeInvocationHandler.class.getName());

    private static final String GENERIC_TYPE_CACHE_MESSAGE = "Caching new generic-types for: {0}";
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
            Class proxyType = invocationInstance.typeToken().getRawType();
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
        
        
        Class returnType = processorUtils.potentialPrimitiveToClass(invocationInstance.typeToken().getRawType());
        boolean isPrimitive = invocationInstance.typeToken().getRawType().isPrimitive();
        
        // 4.) Check that Types passed between handlers are sane and not mismatched.
        //     Throws RuntimeException if something does not match correctly.
        final Map<Integer, Pair<ClassType, ClassType>> requiredChecks;
        try {
            requiredChecks = (Map<Integer, Pair<ClassType, ClassType>>) RUNTIME_METADATA.get(invocationInstance.signature(), () -> {
                return checkTypeConsistency(runtimeRequestHandler,
                        runtimeExecutionHandler,
                        runtimeErrorHandler,
                        runtimeFallbackHandler,
                        runtimeResponseHandler,
                        returnType,
                        isPrimitive);
            });
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex);
        }
        
        for(Map.Entry<Integer, Pair<ClassType, ClassType>> fish : requiredChecks.entrySet()) {
            System.out.println("~~~~~~FOUND: key=" + fish.getKey() + ", value=" + fish.getValue());
        }
        
          
        // 5.) Two things are happening below: we are optionally executing a RequestHandler and 
        //     generating, not optional, an executionContext. The RequestHandler takes in an 
        //     executionContext which is why we have to build/generate it as part of its 
        //     invocation. If we don't execute a RequestHandler we are still required to 
        //     build/generate an executionContext.
        final Object executionContext;
        Class genericExecutionType = genericTypes(runtimeExecutionHandler.getClass())[0];
        if (runtimeRequestHandler != null) {
            Class[] requestTypes = genericTypes(runtimeRequestHandler.getClass()); 
            executionContext = processRequestHandler(runtimeRequestHandler,
                    runtimeExecutionHandler,
                    getInstance(requestTypes[0]),
                    genericExecutionType);
            
            // if necessary check the output of RequestHandler before passing to ExecutionHandler
            Pair<ClassType, ClassType> parsedPair = requiredChecks.get(Constants.REQUEST_HANDLER_TO_EXECUTION_HANDLER_CHECK);
            if (parsedPair != null) {
                try {
                    TypeUtils.parseClassType(executionContext).compare(parsedPair.right());
                } catch (TypeMismatchException tme) {
                    throw new ProcessTimeTypeMismatchException("RequestHandler (" 
                            + runtimeRequestHandler.getClass().getCanonicalName() + ") " 
                            + "outputs do not match ExecutionHandler (" 
                            + runtimeExecutionHandler.getClass().getCanonicalName() + ") inputs.", tme);
                }
            }
        } else {
            executionContext = getInstance(genericExecutionType);
        }
        
        // 5.1 Now that the context has been set the only other handler 
        //     which will accept it is the ErrorHandler. Lets check, if 
        //     necessary, that type-consistency is sane.
        if(runtimeErrorHandler != null) {
            Pair<ClassType, ClassType> parsedPair = requiredChecks.get(Constants.EXECUTION_HANDLER_TO_ERROR_HANDLER_CHECK);
            if (parsedPair != null) {
                try {
                    TypeUtils.parseClassType(executionContext).compare(parsedPair.right());
                } catch (TypeMismatchException tme) {
                    throw new ProcessTimeTypeMismatchException("The 'execution context' " 
                            + "does not match the ErrorHandler (" 
                            + runtimeErrorHandler.getClass().getCanonicalName() + ") inputs.", tme);
                }
            }
        }
            
        // 6.) Pass InvocationInstance to ExecutionHandler for runtime execution.
        final AtomicReference<Object> responseReference = new AtomicReference();
        Throwable invocationException = null;
        try {
            invocationInstance.context(executionContext); // set context for execution
            processExecutionHandler(runtimeExecutionHandler, 
                    responseReference, 
                    invocationInstance);
        } catch (Exception e) {
            invocationException = e;
        }
        
        // 6.1) Because we are successful only 2 paths exist:
        //      
        //          1.) Pass off to ResponseHandler (if applicable)
        //
        //          2.) Return from method invocation
        //
        //      We need to ensure type-consistency for either of 
        //      these scenarios is sane.
        if (invocationException == null) {
            if (runtimeResponseHandler != null) {

                // if necessary check the output of ExecutionHandler before passing to ResponseHandler
                Pair<ClassType, ClassType> parsedPair = requiredChecks.get(Constants.EXECUTION_HANDLER_TO_RESPONSE_HANDLER_CHECK);
                if (parsedPair != null) {
                    try {
                        TypeUtils.parseClassType(responseReference.get()).compare(parsedPair.right());
                    } catch (TypeMismatchException tme) {
                        throw new ProcessTimeTypeMismatchException("ExecutionHandler (" 
                                + runtimeExecutionHandler.getClass().getCanonicalName() + ") " 
                                + "outputs do not match ResponseHandler (" 
                                + runtimeResponseHandler.getClass().getCanonicalName() + ") inputs.", tme);
                    }
                }
            } else {
                
                System.out.println("::::::: NO RESPONSE HANDLER");
                
                // if necessary check the output of ExecutionHandler before returning from method invocation
                Pair<ClassType, ClassType> parsedPair = requiredChecks.get(Constants.EXECUTION_HANDLER_TO_RETURN_VALUE_CHECK);
                if (parsedPair != null) {
                    
                    System.out.println("!!!!!!!! GOT A PAIR");
                    try {       
                                            System.out.println("!!!!!!!! GOT A PAIR2");

                        responseReference.get();
                                
                                                    System.out.println("!!!!!!!! GOT A PAIR3");

                        TypeUtils.parseClassType(responseReference.get()).compare(parsedPair.right());
                    } catch (TypeMismatchException tme) {
                        if (tme.source.equalsIgnoreCase(PrimitiveTypes.NULL.getRawClass().getName())) {
                            if (isPrimitive) {
                                throw new NullNotAllowedException("ExecutionHandler returned NULL while return-value (" 
                                        + invocationInstance.typeToken().getRawType() 
                                        + ") is a primitive.", tme);
                            } else {
                                
                                // Let execution fall through as the return type 
                                // is an Object, and not a primitive, and so can 
                                // accept a null.
                            }
                        } else {
                            throw new ProcessTimeTypeMismatchException("ExecutionHandler (" 
                                    + runtimeExecutionHandler.getClass().getCanonicalName() + ") "
                                    + "outputs do not match expected returnType.", tme);
                        }
                    }
                } else {
                    System.out.println("!!!!!!!! NO PAIR");
                }
            }     
        } 
        
        
        // 7.) Optionally, if exception was found during execution then pass to 
        //     errorHandler for marshalling into some other type of Throwable.
        if (invocationException != null && runtimeErrorHandler != null) {
            invocationException = processErrorHandler(runtimeErrorHandler, 
                    invocationInstance, 
                    invocationException);
        }
        
        // 8.) Optionally, if exception was not previously handled (perhaps 
        //     re-thrown as something else), then pass to fallbackHandler to 
        //     marshall thrown exception into a valid returnValue.
        boolean fallbackInvoked = false;
        if (invocationException != null) {
            if (runtimeFallbackHandler != null) {
                Object newFallbackObject = processFallbackHandler(runtimeFallbackHandler, 
                        invocationInstance, 
                        invocationException);
                
                // if necessary check the output of FallbackHandler output to 
                // ensure type-consistency with the expected returnValue.
                Pair<ClassType, ClassType> parsedPair = requiredChecks.get(Constants.FALLBACK_HANDLER_TO_RETURN_VALUE_CHECK);
                if (parsedPair != null) {
                    try {
                        TypeUtils.parseClassType(newFallbackObject).compare(parsedPair.right());
                    } catch (TypeMismatchException tme) {
                        if (tme.source.equalsIgnoreCase(PrimitiveTypes.NULL.getRawClass().getName())) {
                            if (isPrimitive) {
                                throw new NullNotAllowedException("FallbackHandler returned NULL while return-value (" 
                                        + invocationInstance.typeToken().getRawType() 
                                        + ") is a primitive.", tme);
                            } else {
                                
                                // Let execution fall through as the return type 
                                // is an Object, and not a primitive, and so can 
                                // accept a null.
                            }
                        } else {
                            throw new ProcessTimeTypeMismatchException("FallbackHandler (" 
                                    + runtimeFallbackHandler.getClass().getCanonicalName() + ") "
                                    + "outputs do not match expected returnType.", tme);
                        }
                    }
                }
                
                responseReference.set(newFallbackObject);
                fallbackInvoked = true;
            } else {
                throw Throwables.propagate(invocationException);
            }
        } 
        
        // 9.) Optionally, we can marshall the response from the ExecutionHandler 
        //     into some other valid returnValue.
        if (!fallbackInvoked && runtimeResponseHandler != null) {
            Object newResponseObject = processResponseHandler(runtimeResponseHandler, 
                    responseReference.get(), 
                    invocationInstance);
            
            // if necessary check the ResponseHandler output to 
            // ensure type-consistency with the expected returnValue.
            Pair<ClassType, ClassType> parsedPair = requiredChecks.get(Constants.RESPONSE_HANDLER_TO_RETURN_VALUE_CHECK);
            if (parsedPair != null) {
                try {
                    TypeUtils.parseClassType(newResponseObject).compare(parsedPair.right());
                } catch (TypeMismatchException tme) {
                    if (tme.source.equalsIgnoreCase(PrimitiveTypes.NULL.getRawClass().getName())) {
                        if (isPrimitive) {
                            throw new NullNotAllowedException("ResponseHandler returned NULL while return-value (" 
                                    + invocationInstance.typeToken().getRawType() 
                                    + ") is a primitive.", tme);
                        } else {

                            // Let execution fall through as the return type 
                            // is an Object, and not a primitive, and so can 
                            // accept a null.
                        }
                    } else {
                        throw new ProcessTimeTypeMismatchException("ResponseHandler (" 
                                + runtimeResponseHandler.getClass().getCanonicalName() + ") "
                                + "outputs do not match expected returnType.", tme);
                    }
                }
            }
                
            responseReference.set(newResponseObject);
        } 
        
        return responseReference.get();
    }

    private Object processRequestHandler(final AbstractRequestHandler requestHandler, 
            final AbstractExecutionHandler executionHandler,
            Object executionContext,
            Class genericExecutionType) {
        
        // 1.) Because execution of the RequestHandler is allowed to return a 
        //     different type of Object than what potentially went in, we need 
        //     to check if things ARE different and if so inject potential members.
        Object possibleyNewObject = requestHandler.apply(executionContext);            
        if (possibleyNewObject != null) {

            // 1.2) Re-inject members if hashCodes are different.
            if (executionContext.hashCode() != possibleyNewObject.hashCode()) {                            
                injector.injectMembers(possibleyNewObject);
            } 
        } else {

            // 2.) A returned NULL executionContext from the RequestHandler 
            //      is only allowed IF the ExecutionHandler has an input of 
            //      type java.lang.Void.
            if (!genericExecutionType.equals(PrimitiveTypes.VOID.getRawClass())) {
                throw new NullNotAllowedException("RequestHandler (" 
                        + requestHandler.getClass().getCanonicalName() + ") returned NULL while ExecutionHandler (" 
                        + executionHandler.getClass().getCanonicalName() + ") expects an input of type '" 
                        + genericExecutionType.getCanonicalName() + "'. This is only allowed if the input type is '" 
                        + PrimitiveTypes.VOID.getRawClass().getName() + "'");
            }
        } 
        
        return possibleyNewObject;
    }
    
    private void processExecutionHandler(final AbstractExecutionHandler executionHandler,
            final AtomicReference<Object> responseReference,
            final InvocationInstance invocationInstance) {
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
                    LOGGER.log(Level.FINE, RETRY_RUN_MESSAGE, loggerParams);
                    Object responseObject = executionHandler.apply(invocationInstance);
                    responseReference.set(responseObject); 
                });
    }
    
    private Throwable processErrorHandler(final AbstractErrorHandler errorHandler,
            final InvocationInstance invocationInstance,
            Throwable invocationException) {
        try {
            ErrorWrapper<?> errorWrapper = ErrorWrapper.newInstance(invocationInstance, invocationException);
            Throwable newThrowable = (Throwable) errorHandler.apply(errorWrapper);
            return (newThrowable != null) ? newThrowable : invocationException;
        } catch (Exception propagatedException) {
            return propagatedException;
        }
    }
    
    private Object processFallbackHandler(final AbstractFallbackHandler fallbackHandler,
            final InvocationInstance invocationInstance,
            Throwable invocationException) {
        try {
            FallbackWrapper fallbackWrapper = FallbackWrapper.newInstance(invocationInstance.typeToken(), invocationException);
            return fallbackHandler.apply(fallbackWrapper);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private Object processResponseHandler(final AbstractResponseHandler responseHandler, 
            final Object responseReference,
            final InvocationInstance invocationInstance) {
        ResponseWrapper<?, ?> responseWrapper = ResponseWrapper.newInstance(responseReference, invocationInstance.context(), invocationInstance.typeToken());
        return responseHandler.apply(responseWrapper);
    }
    
    private Map<Integer, Pair<ClassType, ClassType>> checkTypeConsistency(@Nullable AbstractRequestHandler runtimeRequestHandler,
            AbstractExecutionHandler runtimeExecutionHandler,
            @Nullable AbstractErrorHandler runtimeErrorHandler,
            @Nullable AbstractFallbackHandler runtimeFallbackHandler,
            @Nullable AbstractResponseHandler runtimeResponseHandler,
            Class comparisonSafeReturnType,
            boolean isReturnTypePrimitive) {
        
        // The only thing guaranteed to be non-null is the ExecutionHandler 
        // which is why we init it here.   
        final Map<Integer, Pair<ClassType, ClassType>> requiredChecks = Maps.newHashMap();
        ClassType returnType = TypeUtils.parseClassType(comparisonSafeReturnType);        
        ClassType executionTypes = TypeUtils.parseClassType(runtimeExecutionHandler);

        
        // 1.) Check RequestHandler, if applicable, for initial Type as its output  
        //     must match the input of ExecutionHandler.
        if (runtimeRequestHandler != null) {
            ClassType types = TypeUtils.parseClassType(runtimeRequestHandler).subTypeAtIndex(1);                        
            try {
                int index = types.compare(executionTypes.subTypeAtIndex(0));
                if(index > 0) {
                    Pair pair = pairFromParsedTypes(index, types, executionTypes.subTypeAtIndex(0));
                    requiredChecks.put(Constants.REQUEST_HANDLER_TO_EXECUTION_HANDLER_CHECK, pair);
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("RequestHandler (" 
                        + runtimeRequestHandler.getClass().getCanonicalName() + ") " 
                        + "outputs do not match ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") inputs.", tme);
            }
        }
        
        
        // 2.) Check the ErrorHandler input, if applicable, as it must match the 
        //     the input (which is the context in this case) to the ExecutionHandler.
        if (runtimeErrorHandler != null) {
            ClassType types = TypeUtils.parseClassType(runtimeErrorHandler).subTypeAtIndex(0);                        
            try {
                int index = executionTypes.subTypeAtIndex(0).compare(types);
                if (index > 0) {
                    Pair pair = pairFromParsedTypes(index, executionTypes.subTypeAtIndex(0), types);
                    requiredChecks.put(Constants.EXECUTION_HANDLER_TO_ERROR_HANDLER_CHECK, pair);
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") " 
                        + "inputs do not match ErrorHandler (" 
                        + runtimeErrorHandler.getClass().getCanonicalName() + ") inputs.", tme);
            }            
        }

            
        // 3.) Check the FallbackHandler output, if applicable, as it must match 
        //     the expected returnType.
        if (runtimeFallbackHandler != null) {
            ClassType types = TypeUtils.parseClassType(runtimeFallbackHandler).subTypeAtIndex(0);                        
            try {
                int index = types.compare(returnType);
                if(index > 0 || isReturnTypePrimitive) {
                    Pair pair = pairFromParsedTypes(index, types, returnType);
                    requiredChecks.put(Constants.FALLBACK_HANDLER_TO_RETURN_VALUE_CHECK, pair);
                }
            } catch (TypeMismatchException tme) {
                    throw new CheckTimeTypeMismatchException("FallbackHandler (" 
                            + runtimeFallbackHandler.getClass().getCanonicalName() + ") "
                            + "outputs do not match expected returnType.", tme);
            } 
        }

        
        // 4.) Check the ResponseHandler input, if applicable, as it must match  
        //     the ExecutionHandler output. 
        //
        //     Also check the ResponseHandler output as it must match the 
        //     expected returnType. 
        if (runtimeResponseHandler != null) {
            ClassType types = TypeUtils.parseClassType(runtimeResponseHandler);                        
            try {
                int index = executionTypes.subTypeAtIndex(1).compare(types.subTypeAtIndex(0));
                if(index > 0) {
                    Pair pair = pairFromParsedTypes(index, executionTypes.subTypeAtIndex(1), types.subTypeAtIndex(0));
                    requiredChecks.put(Constants.EXECUTION_HANDLER_TO_RESPONSE_HANDLER_CHECK, pair);
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") " 
                        + "outputs do not match ResponseHandler (" 
                        + runtimeResponseHandler.getClass().getCanonicalName() + ") inputs.", tme);
            } 
            
            try {
                int index = types.subTypeAtIndex(1).compare(returnType);
                if(index > 0 || isReturnTypePrimitive) {
                    Pair pair = pairFromParsedTypes(index, types.subTypeAtIndex(1), returnType);
                    requiredChecks.put(Constants.RESPONSE_HANDLER_TO_RETURN_VALUE_CHECK, pair);
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("ResponseHandler (" 
                        + runtimeResponseHandler.getClass().getCanonicalName() + ") "
                        + "outputs do not match expected returnType.", tme);
            } 
            
        } else {
                        
            // 5.) If no ResponseHandler was registered then the ExecutionHandler
            //     is required to return the correct returnType.
            try {
                int index = executionTypes.subTypeAtIndex(1).compare(returnType);
                if(index > 0 || isReturnTypePrimitive) {
                    System.out.println("!!!!!!!! DEFINITELY ADDING: index=" + index);
                    Pair pair = pairFromParsedTypes(index, executionTypes.subTypeAtIndex(1), returnType);
                    requiredChecks.put(Constants.EXECUTION_HANDLER_TO_RETURN_VALUE_CHECK, pair);   
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("ExecutionHandler (" 
                        + runtimeExecutionHandler.getClass().getCanonicalName() + ") "
                        + "outputs do not match expected returnType.", tme);
            }
        }
        
        return requiredChecks;
    }
    
    private Pair<ClassType, ClassType> pairFromParsedTypes(int comparisonValue, ClassType source, ClassType target) {
        switch(comparisonValue) {
            case 0:
                return Pair.of(source, target);
            case 1:
                return Pair.of(source, target);
            case 2:
                return Pair.of(source, target);
            case 3:
                return Pair.of(source, target);
        }
        return null;
    }
    
    /**
     * Get the generic types for a given Class.
     * 
     * @param genericTypeClass the class with potentially generic types.
     * @return array listing generic types in order.
     */
    private Class[] genericTypes(Class genericTypeClass) {        
        try {
            return (Class[]) RUNTIME_METADATA.get(genericTypeClass.getName(), () -> {
                LOGGER.log(Level.CONFIG, GENERIC_TYPE_CACHE_MESSAGE, genericTypeClass.getName());
                return processorUtils.getGenericTypesAsClasses(genericTypeClass);
            });
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex);
        } 
    }
    
    private Object getInstance(Class clazz) {
        Object instance;
        try {
            return (clazz.equals(PrimitiveTypes.VOID.getRawClass())) ? null : injector.getInstance(clazz);                
        } catch (Exception e) {
            instance = processorUtils.newClassInstance(clazz);
            injector.injectMembers(instance);
        }
        return instance;
    }
}
