/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.config;

import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.DefaultExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.handlers.DefaultRuntimeInvocationHandler;
import com.cdancy.api.processor.wrappers.ResponseWrapper;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.wrappers.FallbackWrapper;
import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy
 */
public class HandlerRegistrationModule extends AbstractModule {
        
    private final Class executionHandler;
    private final Class responseHandler;
    private final Class errorHandler;
    private final Class fallbackHandler;
    
    public HandlerRegistrationModule(@Nullable Class<? extends Function<InvocationInstance, ?>> executionHandler, 
            @Nullable Class<? extends Function<ResponseWrapper, ?>> responseHandler,
            @Nullable Class<? extends AbstractErrorHandler> errorHandler,
            @Nullable Class<? extends Function<FallbackWrapper, ?>> fallbackHandler) {
        this.executionHandler = executionHandler;
        this.responseHandler = responseHandler;
        this.errorHandler = errorHandler;
        this.fallbackHandler = fallbackHandler;
    }
            
    @Override 
    protected void configure() {

        bind(AbstractRuntimeInvocationHandler.class).to(DefaultRuntimeInvocationHandler.class);

        Class defaultExecutionHandler = (executionHandler != null) ? executionHandler : DefaultExecutionHandler.class;
        bind(AbstractExecutionHandler.class).to(defaultExecutionHandler);
        
        if (responseHandler != null)
            bind(AbstractResponseHandler.class).to(responseHandler);
        if (errorHandler != null)
            bind(AbstractErrorHandler.class).to(errorHandler);
        if (fallbackHandler != null) 
            bind(AbstractFallbackHandler.class).to(fallbackHandler);     
    }
}