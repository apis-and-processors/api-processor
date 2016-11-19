/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.instance.AnnotatedInstance;
import com.cdancy.api.processor.instance.AnnotatedInstanceCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;

/**
 *
 * @author cdancy
 */
@Singleton
public class DefaultRuntimeInvocationHandler extends AbstractRuntimeInvocationHandler {
        
    @Inject
    AbstractResponseHandler returnValueMarshaller;
    
    @Inject
    AnnotatedInstanceCache annotatedInstanceCache;
    
    @Inject
    AbstractExecutionHandler apiProcessorExecutionHandler;
        
    @Override
    protected Object handleInvocation(Object source, Method method, Object[] args) {
        AnnotatedInstance annotatedInstance = annotatedInstanceCache.getInstance(source.getClass(), method, args);
        ResponseWrapper returnValueWrapper = apiProcessorExecutionHandler.apply(annotatedInstance);
        return returnValueMarshaller.apply(returnValueWrapper);
    }
}
