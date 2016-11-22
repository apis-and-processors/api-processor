/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.wrappers.ErrorWrapper;

/**
 *
 * @author cdancy
 */
public interface AbstractErrorHandler {
    
    /**
     * Handle errors and propagate exception possibly as a new wrapped exception
     * 
     * @param errorWrapper object which wraps the exec-context and exception together
     * @throws java.lang.Exception
     */
    void handleAndPropagate(ErrorWrapper errorWrapper) throws Exception;
}
