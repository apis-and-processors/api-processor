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

package com.cdancy.api.processor.cache;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cdancy.api.processor.ApiProcessorConstants;
import com.cdancy.api.processor.ApiProcessorProperties;

import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.instance.ClassInstance;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.instance.MethodInstance;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.reflect.ReflectionFactory;

/**
 *
 * @author cdancy.
 */
@Singleton
public class ApiProcessorCache {
    
    private static final Logger LOGGER = Logger.getLogger(ApiProcessorCache.class.getName());

    private final Cache<String, Object> cache;
    
    private static final String CLASS_INSTANCE_PREFIX = "ClassInstance@";
    private static final String INVOKABLE_PREFIX = "Invokable@";
    private static final String METHOD_INSTANCE_PREFIX = "MethodInstance@";
    private static final String PROXY_PREFIX = "Proxy@";
    private static final String TYPE_PREFIX = "Type@";
    
    private static final String PROXY_IS_NULL = "proxyInterface cannot be null";
    private static final String PROXY_NOT_INTERFACE = "proxyInterface is not an interface";
    private static final String PROXY_INVOKE_HANDLER_IS_NULL = "invocationHandler cannot be null";
    private static final String PROXY_CACHE_MESSAGE = "Caching new Proxy at: {0}";

    private static final String TYPE_IS_NULL = "clazz cannot be null";
    private static final String TYPE_CACHE_MESSAGE = "Caching new Type at: {0}";
    
    private static final String INVOKABLE_CACHE_MESSAGE = "Caching new Invokable at: {0}";
    private static final String METHOD_INSTANCE_CACHE_MESSAGE = "Caching new MethodInstance at: {0}";
    private static final String CLASS_INSTANCE_CACHE_MESSAGE = "Caching new ClassInstance at: {0}";


    /**
     * Create cache from passed properties.
     * 
     * @param properties the default properties to query for ApiProcessor constants
     */
    public ApiProcessorCache(ApiProcessorProperties properties) {        
        String expireAfterAccess = properties.get(ApiProcessorConstants.CACHE_EXPIRE, "360000");
        cache = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterAccess(Long.valueOf(expireAfterAccess), TimeUnit.MILLISECONDS)
            .build();
    }
            
    /**
     * Create a new type from the passed class interface and invocation handler.
     * 
     * @param <T> the Type of this class/interface.
     * @param proxyInterface class definition for new Type.
     * @param invocationHandler the invocation handler to use for newly created Type.
     * @return newly created Type.
     */
    public <T> T proxyFrom(Class<T> proxyInterface, InvocationHandler invocationHandler) {
        checkNotNull(proxyInterface, PROXY_IS_NULL);
        checkArgument(proxyInterface.isInterface(), PROXY_NOT_INTERFACE);
        checkNotNull(invocationHandler, PROXY_INVOKE_HANDLER_IS_NULL);
        
        String key = (PROXY_PREFIX + proxyInterface.getName()).intern();
        try {
            return (T) cache.get(key, () -> {
                LOGGER.log(Level.CONFIG, PROXY_CACHE_MESSAGE, key);
                return Reflection.newProxy(proxyInterface, invocationHandler);
            });
        } catch (SecurityException | IllegalArgumentException | ExecutionException ex) {
            throw Throwables.propagate(ex);
        } 
    }
    
    /**
     * Create a new Type from the passed bean class (i.e. zero-arg constuctor only) definition.
     * 
     * @param <T> Type of class.
     * @param beanClass zero-arg constructor bean class definition.
     * @return newly created Type.
     */
    public <T> T typeFrom(Class<T> beanClass) {
        checkNotNull(beanClass, TYPE_IS_NULL);
        
        String key = (TYPE_PREFIX + beanClass.getName()).intern();
        try {
            Object obj = cache.get(key, () -> {
                LOGGER.log(Level.CONFIG, TYPE_CACHE_MESSAGE, key);
                Constructor genericConstructor = ReflectionFactory
                        .getReflectionFactory()
                        .newConstructorForSerialization(beanClass, Object.class.getDeclaredConstructors()[0]);

                Constructor<?> ctor = ReflectionFactory
                        .getReflectionFactory()
                        .newConstructorForSerialization(beanClass, genericConstructor);

                return ctor.newInstance();                
            });
            return beanClass.cast(obj);
        } catch (SecurityException | IllegalArgumentException | ExecutionException ex) {
            throw Throwables.propagate(ex);
        } 
    }
    
    /**
     * Create a new Invokable from the passed clazz and method definitions.
     * 
     * @param clazz class definition
     * @param method method definition.
     * @return newly created Invokable.
     */
    private Invokable invokableFrom(Class clazz, Method method) {
        String key = (INVOKABLE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName()).intern();
        try {
            return (Invokable) cache.get(key, () -> {
                LOGGER.log(Level.CONFIG, INVOKABLE_CACHE_MESSAGE, key);
                return TypeToken.of(clazz).method(method);
            });
        } catch (SecurityException | IllegalArgumentException | ExecutionException ex) {
            throw Throwables.propagate(ex);
        } 
    }
    
    /**
     * Create a new MethodInstance from the passed method definition.
     * 
     * @param method method definition.
     * @return newly created MethodInstance.
     */
    private MethodInstance methodInstanceFrom(Method method) {
        String key = (METHOD_INSTANCE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName()).intern(); 
        try {
            return (MethodInstance) cache.get(key, () -> {
                LOGGER.log(Level.CONFIG, METHOD_INSTANCE_CACHE_MESSAGE, key);
                Invokable inv = invokableFrom(method.getDeclaringClass(), method);
                return new MethodInstance(inv.getName(), inv.getAnnotations(), inv.getParameters(), inv.getReturnType());
            });
        } catch (SecurityException | IllegalArgumentException | ExecutionException ex) {
            throw Throwables.propagate(ex);
        } 
    }
        
    /**
     * Create a new ClassInstance from the passed method definition.
     * 
     * @param method method definition.
     * @return newly created ClassInstance.
     */
    public ClassInstance classInstanceFrom(Method method) {
        String key = (CLASS_INSTANCE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName()).intern(); 
        try {
            Invokable inv = invokableFrom(method.getDeclaringClass(), method);
            return (ClassInstance) cache.get(key, () -> {
                LOGGER.log(Level.CONFIG, CLASS_INSTANCE_CACHE_MESSAGE, key);
                return new ClassInstance(inv.getDeclaringClass());
            });
        } catch (SecurityException | IllegalArgumentException | ExecutionException ex) {
            throw Throwables.propagate(ex);
        } 
    }
    
    /**
     * Create a new InvocationInstance from the passed method definition and argument list.
     * 
     * @param method method definition.
     * @param args argument list.
     * @return newly created InvocationInstance.
     */
    public InvocationInstance invocationInstanceFrom(Method method, Object [] args) {  
        ClassInstance classInstance = classInstanceFrom(method);
        MethodInstance methodInstance = methodInstanceFrom(method);
        
        Class<? extends AbstractExecutionHandler> executionHandlerClass = (methodInstance.executionHandler() != null) 
                ? methodInstance.executionHandler() 
                : classInstance.executionHandler();
        Class<? extends AbstractErrorHandler> errorHandlerClass = (methodInstance.errorHandler() != null) 
                ? methodInstance.errorHandler() 
                : classInstance.errorHandler();
        Class<? extends AbstractFallbackHandler> fallbackHandlerClass = (methodInstance.fallbackHandler() != null) 
                ? methodInstance.fallbackHandler() 
                : classInstance.fallbackHandler();
        Class<? extends AbstractResponseHandler> responseHandlerClass = (methodInstance.responseHandler() != null) 
                ? methodInstance.responseHandler() 
                : classInstance.responseHandler();
        
        AbstractExecutionHandler executionHandler = (executionHandlerClass != null) ? typeFrom(executionHandlerClass) : null;
        AbstractErrorHandler errorHandler = errorHandlerClass != null ? typeFrom(errorHandlerClass) : null; 
        AbstractFallbackHandler fallbackHandler = fallbackHandlerClass != null ? typeFrom(fallbackHandlerClass) : null;
        AbstractResponseHandler responseHandler = responseHandlerClass != null ? typeFrom(responseHandlerClass) : null;
                        
        return InvocationInstance.newInstance(classInstance, 
                methodInstance, 
                args,
                executionHandler,
                errorHandler,
                fallbackHandler,
                responseHandler);
    }
}
