/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.wrappers;

import com.google.common.reflect.TypeToken;

/**
 *
 * @author cdancy
 */
public class FallbackWrapper {
    
    public final Throwable runtimeException;
    public final TypeToken expectedReturnType;
    
    public FallbackWrapper (Throwable runtimeException, TypeToken expectedReturnType) {
        this.runtimeException = runtimeException;
        this.expectedReturnType = expectedReturnType;
    }
    
    public Throwable runtimeException() {
        return runtimeException;
    }
    
    public TypeToken expectedReturnType() {
        return expectedReturnType;
    }
    
    public static FallbackWrapper newInstance(Throwable runtimeException, TypeToken expectedReturnType) {
        return new FallbackWrapper(runtimeException, expectedReturnType);
    }
}
