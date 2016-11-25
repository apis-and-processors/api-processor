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
import com.cdancy.api.processor.annotations.ErrorHandler;
import com.cdancy.api.processor.annotations.ExecutionHandler;
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
    
    class LocalErrorHandler extends AbstractErrorHandler {
        @Override
        public Throwable apply(ErrorWrapper object) {
            System.out.println("Hello local error");
            return new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
   
    class LocalExecutionHandler2 extends AbstractExecutionHandler {
        @Override
        public Object apply(InvocationInstance object) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return "fish and fries";
        }
    }
        
    class LocalExecutionHandler extends AbstractExecutionHandler {
        @Override
        public Object apply(InvocationInstance object) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            throw new RuntimeException("fish!!!!");
        }
    }
        
    class LocalFallbackHandler extends AbstractFallbackHandler {
        @Override
        public Object apply(FallbackWrapper object) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
            
    class LocalResponseHandler extends AbstractResponseHandler {
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
        abstract String helloWorld(@Nullable @ArgsValue("message") String message, int number, String monkey);
    }
    
    @Test
    public void testSomeLibraryMethod() {
        
        System.out.println("----->Starting...");
        
        HelloWorld helloWorld = ApiProcessor.builder().scanClasspath().build().get(HelloWorld.class);
        Object returnValue = helloWorld.helloWorld("fish", 123, null);

        System.out.println("-----> ReturnValue=" + returnValue);
        System.out.println("----->Ending...");
        
        Set<Class> fish = new HashSet<>();
    }
}


