/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.config;

import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.proxy.ProxyHelper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import java.util.Set;

/**
 *
 * @author cdancy
 */
public class ApiRegistrationModule extends AbstractModule {

    private final Set<Class> apis;
    private final AbstractRuntimeInvocationHandler abstractRuntimeInvocationHandler;
            
    public ApiRegistrationModule(Set<Class> apis, AbstractRuntimeInvocationHandler abstractRuntimeInvocationHandler) {
        this.apis = ImmutableSet.copyOf(apis);
        this.abstractRuntimeInvocationHandler = abstractRuntimeInvocationHandler;
    }
        
    @Override 
    protected void configure() {
        apis.stream().forEach( entry -> {
            bind(entry).toInstance(ProxyHelper.instanceFromInterface(entry, abstractRuntimeInvocationHandler));
        });
    }
}