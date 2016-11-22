/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.wrappers;

import javax.annotation.Nullable;

/**
 *
 * @author cdancy
 */
public class ErrorWrapper {
    
    public final Object executionContext;
    public final Throwable thrownException;
    
    public ErrorWrapper (@Nullable Object executionContext, Throwable thrownException) {
        this.executionContext = executionContext;
        this.thrownException = thrownException;
    }
    
    public Object executionContext() {
        return executionContext;
    }
    
    public Throwable thrownException() {
        return thrownException;
    }
    
    public static ErrorWrapper newInstance(Object executionContext, Throwable thrownException) {
        return new ErrorWrapper(executionContext, thrownException);
    }
}
