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

import static com.google.common.base.Preconditions.checkNotNull;

import com.cdancy.api.processor.instance.ClassInstance;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.instance.MethodInstance;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.reflect.ReflectionFactory;

/**
 *
 * @author cdancy.
 */
public class ProcessorCache {
    
    private static final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .weakValues()
            .build();
    
    private static final Logger logger = Logger.getLogger(ProcessorCache.class.getName());

    private static final String CLASS_INSTANCE_PREFIX = "ClassInstance@";
    private static final String INVOKABLE_PREFIX = "Invokable@";
    private static final String METHOD_INSTANCE_PREFIX = "MethodInstance@";
    private static final String TYPE_PREFIX = "Type@";
    
    /**
     * Create a new Type from the passed bean class (i.e. zero-arg constuctor only) definition.
     * 
     * @param <T> Type of class.
     * @param beanClass zero-arg constructor bean class definition.
     * @return newly created Type.
     */
    public static <T> T typeFrom(Class<T> beanClass) {
        checkNotNull(beanClass, "clazz cannot be null");
        
        String key = TYPE_PREFIX + beanClass.getName();
        try {
            Object obj = cache.get(key, () -> {
                logger.log(Level.INFO, "Caching new Type at: " + key);
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
    public static Invokable invokableFrom(Class clazz, Method method) {
        String key = INVOKABLE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName();
        try {
            return (Invokable) cache.get(key, () -> {
                logger.log(Level.INFO, "Caching new Invokable at: " + key);
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
    public static MethodInstance methodInstanceFrom(Method method) {
        String key = METHOD_INSTANCE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName(); 
        try {
            return (MethodInstance) cache.get(key, () -> {
                logger.log(Level.INFO, "Caching new MethodInstance at: " + key);
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
    public static ClassInstance classInstanceFrom(Method method) {
        String key = CLASS_INSTANCE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName(); 
        try {
            Invokable inv = invokableFrom(method.getDeclaringClass(), method);
            return (ClassInstance) cache.get(key, () -> {
                logger.log(Level.INFO, "Caching new ClassInstance at: " + key);
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
    public static InvocationInstance invocationInstanceFrom(Method method, Object [] args) {  
        ClassInstance classInstance = classInstanceFrom(method);
        MethodInstance methodInstance = methodInstanceFrom(method);
        return InvocationInstance.newInstanceFrom(classInstance, methodInstance, args);
    }
}
