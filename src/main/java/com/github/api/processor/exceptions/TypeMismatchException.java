/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.exceptions;

/**
 * Thrown when generic types of 2 classes are found to be different 
 * but were expected to be the same.
 * 
 * @author cdancy
 */
public class TypeMismatchException extends RuntimeException {
    
    public TypeMismatchException() {
        super();
    }
    
    public TypeMismatchException(String s) {
        super(s);
    }
    
    public TypeMismatchException(String s, Throwable throwable) {
        super(s, throwable);
    }
    
    public TypeMismatchException(Throwable throwable) {
        super(throwable);
    }
}
