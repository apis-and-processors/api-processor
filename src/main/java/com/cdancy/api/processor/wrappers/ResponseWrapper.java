/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.wrappers;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy
 */
public class ResponseWrapper {
    
    public final Object returnValue;
    public final TypeToken expectedReturnType;
    
    public ResponseWrapper (@Nullable Object returnValue, TypeToken expectedReturnType) {
        this.returnValue = returnValue;
        this.expectedReturnType = expectedReturnType;
    }
    
    public Object returnValue() {
        return returnValue;
    }
    
    public TypeToken expectedReturnType() {
        return expectedReturnType;
    }
    
    public static ResponseWrapper newInstance(Object returnValue, TypeToken expectedReturnType) {
        return new ResponseWrapper(returnValue, expectedReturnType);
    }
}
