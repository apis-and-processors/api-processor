/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.proxy;

/**
 *
 * @author cdancy
 */
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import java.util.Map;

public class InvokableCache {

    private static final Map<String, Invokable> invokableCache = Maps.newConcurrentMap();
    
    public static synchronized Invokable invokable(Class clazz, Method method) {
        String key = clazz.getName() + "@" + method.getName();
        Invokable invokable = invokableCache.get(key);
        if (invokable == null) {
            final Invokable newInvokable = TypeToken.of(clazz).method(method);
            invokableCache.put(key, newInvokable);
            invokable = newInvokable;
        }
        return invokable;
    }
}
