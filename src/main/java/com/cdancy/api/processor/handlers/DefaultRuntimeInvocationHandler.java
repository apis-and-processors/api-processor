/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.wrappers.ResponseWrapper;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.cache.ProcessorCache;
import com.cdancy.api.processor.wrappers.ErrorWrapper;
import com.cdancy.api.processor.wrappers.FallbackWrapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy.
 */
@Singleton
public class DefaultRuntimeInvocationHandler extends AbstractRuntimeInvocationHandler {
    
    @Inject
    AbstractExecutionHandler abstractExecutionHandler;
    
    @Inject(optional = true)
    @Nullable
    AbstractErrorHandler abstractErrorHandler;
        
    @Inject(optional = true)
    @Nullable
    AbstractFallbackHandler abstractFallbackHandler;
 
    @Inject(optional = true)
    @Nullable
    AbstractResponseHandler abstractResponseHandler;
        
    @Override
    protected Object handleInvocation(Object source, Method method, Object[] args) {
        
        // 1.) Get/Build InvocationInstance from cache.
        final InvocationInstance invocationInstance = ProcessorCache.invocationInstanceFrom(method, args);
        
        // 2.) Initialize handlers, if present, for runtime execution
        final AbstractExecutionHandler runtimeExecutionHandler = (invocationInstance.executionHandler() != null) 
                ? invocationInstance.executionHandler() 
                : abstractExecutionHandler;
        final AbstractErrorHandler runtimeErrorHandler = (invocationInstance.errorHandler() != null) 
                ? invocationInstance.errorHandler() 
                : abstractErrorHandler;
        final AbstractFallbackHandler runtimeFallbackHandler = (invocationInstance.fallbackHandler() != null) 
                ? invocationInstance.fallbackHandler() 
                : abstractFallbackHandler;
        final AbstractResponseHandler runtimeResponseHandler = (invocationInstance.responseHandler() != null) 
                ? invocationInstance.responseHandler() 
                : abstractResponseHandler;
        
        
        // 3.) Pass InvocationInstance to ExecutionHandler for runtime execution.
        Object executionResponse = null;
        Throwable invocationException = null;
        try {
            executionResponse = runtimeExecutionHandler.apply(invocationInstance);            
        } catch (Exception e) {
            invocationException = e;
        }
        
        // 4.) Optionally, if exception was found during execution then pass to errorHandler for marshalling
        if (invocationException != null && runtimeErrorHandler != null) {
            try {
                Throwable possibleThrowable = runtimeErrorHandler.apply(ErrorWrapper.newInstance(invocationInstance, invocationException));
                if (possibleThrowable != null) {
                    invocationException = possibleThrowable;
                }
            } catch (Exception propagatedException) {
                invocationException = propagatedException;
            }
        }
        
        // 5.) Optionally, if exception was not previously handled (perhaps re-thrown as something else), then pass to fallbackHandler
        boolean fallbackInvoked = false;
        if (invocationException != null && runtimeFallbackHandler != null) {
            try {
                executionResponse = runtimeFallbackHandler.apply(FallbackWrapper.newInstance(invocationException, invocationInstance.returnType()));
                fallbackInvoked = true;
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        
        // 6.) Optionally, we can marshall the response to some other object 
        if (!fallbackInvoked && runtimeResponseHandler != null) {
            executionResponse = runtimeResponseHandler.apply(ResponseWrapper.newInstance(executionResponse, invocationInstance.returnType()));
        } 
        
        return executionResponse;
    }
}
