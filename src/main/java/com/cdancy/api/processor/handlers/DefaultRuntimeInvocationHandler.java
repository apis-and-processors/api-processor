/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.wrappers.ResponseWrapper;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.cache.ProcessorCache;
import com.cdancy.api.processor.wrappers.ErrorWrapper;
import com.cdancy.api.processor.wrappers.FallbackWrapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy
 */
@Singleton
public class DefaultRuntimeInvocationHandler extends AbstractRuntimeInvocationHandler {
    
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
        
    @Override
    protected Object handleInvocation(Object source, Method method, Object[] args) {
        
        // 1.) Get/Build InvocationInstance from cache.
        final InvocationInstance invocationInstance = ProcessorCache.invocationInstanceFrom(method, args);
        
        // 2.) Initialize handlers, if present, for runtime execution
        AbstractExecutionHandler runtimeExecutionHandler = (invocationInstance.executionHandler() != null) ? 
                invocationInstance.executionHandler() : 
                abstractExecutionHandler;
        AbstractErrorHandler runtimeErrorHandler = (invocationInstance.errorHandler() != null) ? 
                invocationInstance.errorHandler() : 
                abstractErrorHandler;
        AbstractFallbackHandler runtimeFallbackHandler = (invocationInstance.fallbackHandler() != null) ? 
                invocationInstance.fallbackHandler() : 
                abstractFallbackHandler;
        AbstractResponseHandler runtimeResponseHandler = (invocationInstance.responseHandler() != null) ? 
                invocationInstance.responseHandler() : 
                abstractResponseHandler;
        
        
        // 3.) Pass InvocationInstance to ExecutionHandler for runtime execution.
        Object executionResponse = null;
        Throwable invocationException = null;
        try {
            executionResponse = runtimeExecutionHandler.apply(invocationInstance);            
        } catch (Exception e) {
            invocationException = e;
        }
        
        // 4.) Optionally, if exception was found during execution then pass to errorHandler for marshalling
        if (invocationException != null && runtimeErrorHandler != null) {
            try {
                Throwable possibleThrowable = runtimeErrorHandler.apply(ErrorWrapper.newInstance(invocationInstance, invocationException));
                if (possibleThrowable != null) {
                    invocationException = possibleThrowable;
                }
            } catch (Exception propagatedException) {
                  invocationException = propagatedException;
            }
        }
        
        // 5.) Optionally, if exception was not previously handled (perhaps re-thrown as something else), then pass to fallbackHandler
        boolean fallbackInvoked = false;
        if (invocationException != null && runtimeFallbackHandler != null) {
            try {
                executionResponse = runtimeFallbackHandler.apply(FallbackWrapper.newInstance(invocationException, invocationInstance.returnType()));
                fallbackInvoked = true;
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        
        // 6.) Optionally, we can marshall the response to some other object 
        if (!fallbackInvoked && runtimeResponseHandler != null) {
            executionResponse = runtimeResponseHandler.apply(ResponseWrapper.newInstance(executionResponse, invocationInstance.returnType()));
        } 
        
        return executionResponse;
    }
}
