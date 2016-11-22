/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor;

import com.cdancy.api.processor.annotations.Api;
import com.cdancy.api.processor.config.ApiRegistrationModule;
import com.cdancy.api.processor.config.HandlerRegistrationModule;
import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractRuntimeInvocationHandler;
import com.cdancy.api.processor.wrappers.ResponseWrapper;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.utils.ClasspathUtils;
import com.cdancy.api.processor.wrappers.FallbackWrapper;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

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
        //private final List<Module> modules = Lists.newArrayList();
        private boolean scanClasspath = false;
        
        private Class<? extends Function<InvocationInstance, ?>> executionHandler;
        private Class<? extends Function<ResponseWrapper, ?>> responseHandler;
        private Class<? extends AbstractErrorHandler> errorHandler;
        private Class<? extends Function<FallbackWrapper, ?>> fallbackHandler;
        
        public Builder api(Class clazz) {
            checkNotNull(clazz, "api class cannot be null");
            checkArgument(clazz.isInterface(), "api class must be an interface");
            this.apis.add(clazz);
            return this;
        }
        
        public Builder modules(Module module) {
            //this.modules.add(checkNotNull(module, "module cannot be null"));
            return this;
        }
                
        public Builder scanClasspath() {
            this.scanClasspath = true;
            return this;
        }
        
        public Builder executionHandler(Class<? extends Function<InvocationInstance, ?>> executionHandler) {
            this.executionHandler = checkNotNull(executionHandler, "executionHandler cannot be null");
            return this;
        }
                
        public Builder responseHandler(Class<? extends Function<ResponseWrapper, ?>> responseHandler) {
            this.responseHandler = checkNotNull(responseHandler, "responseHandler cannot be null");
            return this;
        }
        
        public Builder errorHandler(Class<? extends AbstractErrorHandler> errorHandler) {
            this.errorHandler = checkNotNull(errorHandler, "errorHandler cannot be null");
            return this;
        }
               
        public Builder fallbackHandler(Class<? extends Function<FallbackWrapper, ?>> fallbackHandler) {
            this.fallbackHandler = checkNotNull(fallbackHandler, "fallbackHandler cannot be null");
            return this;
        }
        
        public ApiProcessor build() {
            
            // 1.) Gather all Api's passed in and on classpath
            Set<Class> builtApis = Sets.newHashSet(apis);
            if (this.scanClasspath) {
                builtApis.addAll(ClasspathUtils.classesAnnotatedWith(Api.class));
            }
            
            checkArgument(builtApis.size() > 0, "must have at least 1 api to initialize processor");
            builtApis.stream().forEach(entry -> {
                logger.log(Level.INFO, "Found Api @ {0}", entry.getName());
            });
                        
            // 2.) Create injector from modules and build ApiProcessor
            final Injector handlerInjector = Guice.createInjector(new HandlerRegistrationModule(executionHandler, responseHandler, errorHandler, fallbackHandler));
            AbstractRuntimeInvocationHandler apiProcessorInvocationHandler = handlerInjector.getInstance(AbstractRuntimeInvocationHandler.class);
            final Injector apiInjector = handlerInjector.createChildInjector(new ApiRegistrationModule(builtApis, apiProcessorInvocationHandler));
            return new ApiProcessor(apiInjector);
        }
    }
}
