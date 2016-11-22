/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.wrappers.ResponseWrapper;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.instance.InvocationInstanceCache;
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
    AbstractResponseHandler abstractResponseHandler;
    
    @Inject(optional = true)
    @Nullable
    AbstractErrorHandler abstractErrorHandler;
        
    @Inject(optional = true)
    @Nullable
    AbstractFallbackHandler abstractFallbackHandler;
            
    @Inject
    InvocationInstanceCache invocationInstanceCache;
        
    @Override
    protected Object handleInvocation(Object source, Method method, Object[] args) {
        
        // 1.) Get/Build InvocationInstance from cache.
        final InvocationInstance annotatedInstance = invocationInstanceCache.getInstance(source.getClass(), method, args);
        
        // 2.) Pass InvocationInstance to ExecutionHandler for runtime execution.
        Object executionResponse = null;
        Throwable invocationException = null;
        try {
            executionResponse = abstractExecutionHandler.apply(annotatedInstance);            
        } catch (Exception e) {
            invocationException = e;
        }
        
        // 3.) Optionally, if exception was found during execution then pass to errorHandler
        if (invocationException != null && abstractErrorHandler != null) {
            try {
                abstractErrorHandler.handleAndPropagate(ErrorWrapper.newInstance(annotatedInstance, invocationException));
            } catch (Exception propagatedException) {
                  invocationException = propagatedException;
            }
        }
        
        // 4.) Optionally, if exception was not previously handled (perhaps re-thrown as something else), then pass to fallbackHandler
        boolean fallbackInvoked = false;
        if (invocationException != null && abstractFallbackHandler != null) {
            try {
                executionResponse = abstractFallbackHandler.apply(FallbackWrapper.newInstance(invocationException, annotatedInstance.returnType()));
                fallbackInvoked = true;
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        
        // 5.) Optionally, we can marshall the response to some other object 
        if (!fallbackInvoked && abstractResponseHandler != null) {
            executionResponse = abstractResponseHandler.apply(ResponseWrapper.newInstance(executionResponse, annotatedInstance.returnType()));
        } 
        
        return executionResponse;
    }
}
