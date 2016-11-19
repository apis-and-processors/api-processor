/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor;

import com.cdancy.api.processor.annotations.Api;
import com.cdancy.api.processor.config.ApiRegistrationModule;
import com.cdancy.api.processor.config.HandlerRegistrationModule;
import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import java.util.List;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cdancy
 */
public class ApiProcessor {
    
    private final Injector injector;
    
    private ApiProcessor(Injector injector) {
        this.injector = injector;
    }
    
    public <T> T get(Class<T> clazz) {
        return injector.getInstance(clazz);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
    
        private final Logger logger = Logger.getLogger(ApiProcessor.class.getName());
        private final Set<Class> apis = Sets.newHashSet();
        private final List<Module> modules = Lists.newArrayList();
        private boolean scanClasspath = false;
        
        public Builder api(Class clazz) {
            checkNotNull(clazz, "api class cannot be null");
            checkArgument(clazz.isInterface(), "api class must be an interface");
            this.apis.add(clazz);
            return this;
        }
        
        public Builder modules(Module module) {
            checkNotNull(module, "module cannot be null");
            this.modules.add(module);
            return this;
        }
                
        public Builder scanClasspath() {
            this.scanClasspath = true;
            return this;
        }
        
        public ApiProcessor build() {
            
            // gather all Api's passed in and on classpath
            Set<Class> builtApis = Sets.newHashSet(apis);
            if (this.scanClasspath) {
               new FastClasspathScanner().
                       matchClassesWithAnnotation(Api.class, c -> {
                           if (!builtApis.contains(c)) {
                               builtApis.add(c);
                               logger.log(Level.INFO, "Found Api @ {0}", c.getName());
                           } else {
                               logger.log(Level.WARNING, "Api @ {0} was previously loaded", c.getName());
                           }
                       }).scan();
            }
            
            checkArgument(builtApis.size() > 0, "must have at least 1 api to initialize processor");
                        
            final Injector handlerInjector = Guice.createInjector(new HandlerRegistrationModule());
            AbstractRuntimeInvocationHandler apiProcessorInvocationHandler = handlerInjector.getInstance(AbstractRuntimeInvocationHandler.class);
            final Injector apiInjector = handlerInjector.createChildInjector(new ApiRegistrationModule(builtApis, apiProcessorInvocationHandler));
            return new ApiProcessor(apiInjector);
        }
    }
}
