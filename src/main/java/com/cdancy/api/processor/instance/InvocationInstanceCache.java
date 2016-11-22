/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.instance;

import com.cdancy.api.processor.proxy.InvokableCache;
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 * @author cdancy
 */
@Singleton
public class InvocationInstanceCache {
   
    private final Map<String, ClassInstance> classCache = Maps.newConcurrentMap();
    
    public synchronized InvocationInstance getInstance(Class clazz, Method method, Object [] args) {  
        ClassInstance classInstance = classCache.get(clazz.getName());
        if (classInstance == null) {
            final Invokable newInvokable = InvokableCache.invokable(clazz, method);
            final ClassInstance newClassInstance = new ClassInstance(newInvokable.getDeclaringClass());
            classCache.put(clazz.getName(), newClassInstance);
            classInstance = newClassInstance;
        } 
        return InvocationInstance.newInstanceFrom(classInstance, classInstance.get(clazz, method), args);
    }
}
