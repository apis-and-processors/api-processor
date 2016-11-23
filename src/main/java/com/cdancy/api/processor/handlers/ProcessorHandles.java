/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import javax.annotation.Nullable;

/**
 *
 * @author cdancy
 */
public interface ProcessorHandles {
    
    @Nullable
    Class<? extends AbstractExecutionHandler> executionHandler();
    
    @Nullable
    Class<? extends AbstractErrorHandler> errorHandler();
    
    @Nullable
    Class<? extends AbstractFallbackHandler> fallbackHandler();
 
    @Nullable
    Class<? extends AbstractResponseHandler> responseHandler();
}
