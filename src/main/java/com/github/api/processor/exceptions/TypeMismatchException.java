/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.exceptions;

/**
 * 
 * @author cdancy
 */
public class TypeMismatchException extends RuntimeException {
    
    public final String source;
    public final String target;
    
    public TypeMismatchException(String message, String source, String target) {
        super(message);
        this.source = source;
        this.target = target;
    }
    
    public TypeMismatchException(String message, String source, String target, Throwable throwable) {
        super(message, throwable);
        this.source = source;
        this.target = target;
    }
}
