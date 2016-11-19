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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.reflect.Reflection;
import com.google.inject.Singleton;
import java.lang.reflect.InvocationHandler;

@Singleton
public class ProxyHelper {
    
    public static <T> T instanceFromInterface(Class<T> proxyInterface, InvocationHandler invocationHandler) {
        checkNotNull(proxyInterface, "proxyInterfaced cannot be null");
        checkNotNull(invocationHandler, "invocationHandler cannot be null");
        return Reflection.newProxy(proxyInterface, invocationHandler);       
    }
}
