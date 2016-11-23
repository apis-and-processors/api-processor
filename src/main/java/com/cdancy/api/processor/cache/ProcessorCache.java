/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cdancy.api.processor.instance.ClassInstance;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        
    private final static String INVOKABLE_PREFIX = "Invokable@";
    private final static String INVOCATION_INSTANCE_PREFIX = "InvocationInstance@";
    private final static String TYPE_PREFIX = "Type@";
    
    public static <T> T typeFrom(Class<T> zeroArgConstructorBean) {
        checkNotNull(zeroArgConstructorBean, "clazz cannot be null");
        
        String key = TYPE_PREFIX + zeroArgConstructorBean.getName();
        try {            
            
            Object obj = cache.getIfPresent(key);
            if (obj == null) {
                Constructor genericConstructor = ReflectionFactory
                        .getReflectionFactory()
                        .newConstructorForSerialization(zeroArgConstructorBean, Object.class.getDeclaredConstructors()[0]);

                Constructor<?> ctor = ReflectionFactory
                        .getReflectionFactory()
                        .newConstructorForSerialization(zeroArgConstructorBean, genericConstructor);

                final Object newObj = ctor.newInstance();
                cache.put(key, newObj);
                obj = newObj;
            } 
            return zeroArgConstructorBean.cast(obj);
        } catch (SecurityException | InstantiationException | 
                IllegalAccessException | IllegalArgumentException | 
                InvocationTargetException ex) {
            throw Throwables.propagate(ex);
        } 
    }
    
    public static Invokable invokableFrom(Class clazz, Method method) {
        String key = INVOKABLE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName();
        Invokable invokable = (Invokable)cache.getIfPresent(key);
        if (invokable == null) {
            final Invokable newInvokable = TypeToken.of(clazz).method(method);
            cache.put(key, newInvokable);
            invokable = newInvokable;
        } 
        return invokable;
    }
    
    public static InvocationInstance invocationInstanceFrom(Class proxy, Method method, Object [] args) {  
        String key = INVOCATION_INSTANCE_PREFIX + method.getDeclaringClass().getName() + "@" + method.getName();
        ClassInstance classInstance = (ClassInstance)cache.getIfPresent(key);
        if (classInstance == null) {
            final Invokable newInvokable = ProcessorCache.invokableFrom(proxy, method);
            final ClassInstance newClassInstance = new ClassInstance(newInvokable.getDeclaringClass());
            cache.put(key, newClassInstance);
            classInstance = newClassInstance;
        } 
        return InvocationInstance.newInstanceFrom(classInstance, classInstance.get(proxy, method), args);
    }
}
