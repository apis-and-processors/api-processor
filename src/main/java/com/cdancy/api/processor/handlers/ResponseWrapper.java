/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.google.common.reflect.TypeToken;

/**
 *
 * @author cdancy
 */
public class ResponseWrapper {
    
    public final Object returnValue;
    public final TypeToken expectedType;
    
    public ResponseWrapper (Object returnValue, TypeToken expectedType) {
        this.returnValue = returnValue;
        this.expectedType = expectedType;
    }
    
    public Object returnValue() {
        return returnValue;
    }
    
    public TypeToken expectedType() {
        return expectedType;
    }
    
    public static ResponseWrapper newInstance(Object returnValue, TypeToken expectedType) {
        return new ResponseWrapper(returnValue, expectedType);
    }
}
