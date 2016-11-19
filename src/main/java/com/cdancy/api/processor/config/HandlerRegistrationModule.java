/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.config;

import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.DefaultExecutionHandler;
import com.cdancy.api.processor.handlers.DefaultResponseHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.handlers.DefaultRuntimeInvocationHandler;
import com.google.inject.AbstractModule;

/**
 *
 * @author cdancy
 */
public class HandlerRegistrationModule extends AbstractModule {
        
    @Override 
    protected void configure() {
        bind(AbstractRuntimeInvocationHandler.class).to(DefaultRuntimeInvocationHandler.class);
        bind(AbstractExecutionHandler.class).to(DefaultExecutionHandler.class);
        bind(AbstractResponseHandler.class).to(DefaultResponseHandler.class);
    }
}