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

package com.cdancy.api.processor;

import com.cdancy.api.processor.annotations.Api;
import com.cdancy.api.processor.annotations.Args;
import com.cdancy.api.processor.annotations.ArgsValue;
import com.cdancy.api.processor.annotations.Delegate;
import com.cdancy.api.processor.annotations.ExecutionHandler;
import com.cdancy.api.processor.annotations.FallbackHandler;
import com.cdancy.api.processor.handlers.AbstractErrorHandler;
import com.cdancy.api.processor.handlers.AbstractExecutionHandler;
import com.cdancy.api.processor.handlers.AbstractFallbackHandler;
import com.cdancy.api.processor.handlers.AbstractResponseHandler;
import com.cdancy.api.processor.instance.InvocationInstance;
import com.cdancy.api.processor.wrappers.ErrorWrapper;
import com.cdancy.api.processor.wrappers.FallbackWrapper;
import com.cdancy.api.processor.wrappers.ResponseWrapper;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.testng.annotations.Test;

public class ApiProcessorTest {
    
    class LocalErrorHandler extends AbstractErrorHandler<String> {
        @Override
        public Throwable apply(ErrorWrapper<String> object) {
            
            System.out.println("Hello local error");
            return new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
   
    class LocalExecutionHandler2 extends AbstractExecutionHandler<Object> {
        @Override
        public Object apply(InvocationInstance object) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return "fish and fries";
        }
    }
        
    class LocalExecutionHandler extends AbstractExecutionHandler<Object> {
        @Override
        public Object apply(InvocationInstance object) {
            
            System.out.println("+++++++++++ FIRST ANNO: " + object.firstClassAnnotation(Args.class));
            System.out.println("+++++++++++ LAST ANNO: " + object.lastClassAnnotation(Args.class));
            
            System.out.println("++++++++++param count: " + object.parameterCount());
            
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            throw new RuntimeException("fish!!!!");
        }
    }
        
    class LocalFallbackHandler extends AbstractFallbackHandler<String> {
        @Override
        public String apply(FallbackWrapper object) {
            
            System.out.println("Exepected return-type: " + object.returnType);
            System.out.println("Thrown exception: " + object.thrownException.getClass());
            System.out.println("Falling back to null");
            return null;
        }
    }
            
    class LocalResponseHandler extends AbstractResponseHandler<Object> {
        @Override
        public Object apply(ResponseWrapper object) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
                
    @Args( { "git" } )
    static interface Tigers {
        
    }
        
    @Args( { "commit" } )
    static interface Bears extends Tigers {
        
    }
    
    @Api
    @Args( { "-am" } )
    @ExecutionHandler(LocalExecutionHandler2.class)
    static interface HelloWorld extends Bears {
        
        @Args( { "{message}" } )
        @ExecutionHandler(LocalExecutionHandler.class)
        @FallbackHandler(LocalFallbackHandler.class)
        String helloWorld(@Nullable @ArgsValue("message") String message, int number, String monkey);
    }
    
    @Api
    static interface HelloWorldApi {
        
        @Delegate
        HelloWorld helloWorld();
    }
    
    @Test
    public void testSomeLibraryMethod() {
        
        System.out.println("----->Starting...");
        
        HelloWorldApi helloWorldApi = ApiProcessor.builder()
                .scanClasspath()
                .properties(ApiProcessorConstants.CACHE_EXPIRE, "10000").build()
                .get(HelloWorldApi.class);
        HelloWorld helloWorld = helloWorldApi.helloWorld();
        Object returnValue = helloWorld.helloWorld("fish", 123, null);

        System.out.println("-----> ReturnValue=" + returnValue);
        System.out.println("----->Ending...");
        
        Set<Class> fish = new HashSet<>();
    }
}


