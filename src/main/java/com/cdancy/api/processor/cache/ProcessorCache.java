/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author cdancy
 */
public class ProcessorCache {
    
    private final static Cache<String, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .weakValues()
        .build();
    
    private final static Logger logger = Logger.getLogger(ProcessorCache.class.getName());

    private final static String CLASS_INSTANCE_PREFIX = "ClassInstance@";
    private final static String INVOKABLE_PREFIX = "Invokable@";
    private final static String METHOD_INSTANCE_PREFIX = "MethodInstance@";
    private final static String TYPE_PREFIX = "Type@";
    
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
    
    public static InvocationInstance invocationInstanceFrom(Method method, Object [] args) {  
        ClassInstance classInstance = classInstanceFrom(method);
        return InvocationInstance.newInstanceFrom(classInstance, classInstance.get(method.getDeclaringClass(), method), args);
    }
}
