/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.config;

import com.cdancy.api.processor.instance.InvocationInstanceCache;
import com.google.inject.AbstractModule;

/**
 *
 * @author cdancy
 */
public class StandaloneModules extends AbstractModule {
        
    @Override 
    protected void configure() {
        bind(InvocationInstanceCache.class);
    }
}