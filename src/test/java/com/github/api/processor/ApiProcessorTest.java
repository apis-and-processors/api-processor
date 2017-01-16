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

package com.github.api.processor;


import com.github.api.processor.annotations.Api;
import com.github.api.processor.annotations.Args;
import com.github.api.processor.annotations.ArgsValue;
import com.github.api.processor.annotations.Delegate;
import com.github.api.processor.annotations.ErrorHandler;
import com.github.api.processor.annotations.ExecutionHandler;
import com.github.api.processor.annotations.FallbackHandler;
import com.github.api.processor.annotations.RequestHandler;
import com.github.api.processor.annotations.ResponseHandler;
import com.github.api.processor.handlers.AbstractErrorHandler;
import com.github.api.processor.handlers.AbstractExecutionHandler;
import com.github.api.processor.handlers.AbstractFallbackHandler;
import com.github.api.processor.handlers.AbstractRequestHandler;
import com.github.api.processor.handlers.AbstractResponseHandler;
import com.github.api.processor.instance.InvocationInstance;
import com.github.api.processor.wrappers.ErrorWrapper;
import com.github.api.processor.wrappers.FallbackWrapper;
import com.github.api.processor.wrappers.ResponseWrapper;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import javax.annotation.Nullable;
import org.testng.annotations.Test;
import org.testng.collections.Maps;

public class ApiProcessorTest {
    
    private class SpecialBean {
        private Map<String, String> properties = Maps.newHashMap();
        
        public SpecialBean() {
            properties = Maps.newHashMap();
        }
        
        public Map<String, String> props() {
            return properties;
        }
    }
        
    class LocalRequestHandler extends AbstractRequestHandler<Integer, Object> {
        @Override
        public Object apply(Integer object) {
            if (object == null) {
                System.out.println("it is null");
            } else {
                System.out.println("______________REQUESTHANDLER: " + object.getClass());
            }
            return 123;
        }
    }
        
    class LocalExecutionHandler extends AbstractExecutionHandler<Integer, Integer> {
        @Override
        public Integer apply(InvocationInstance<Integer> object) {
            
            System.out.println("Context: " + object.context());
            //System.out.println("properties: " + object.context().props());
            //object.context().properties.put("fish", "bear");
            
            ImmutableList<Args> argsList = object.combinedAnnotations(Args.class);
            for(int i = argsList.size() -1; i >= 0; i--) {
                Args param = argsList.get(i);
                System.out.println("-----------found: " + param);
                for (int j = 0; j < param.value().length; j++) {
                    System.out.println("---value=" + param.value()[j]);
                }                
            }
            System.out.println("+++++++++++ FIRST ANNO: " + object.firstClassAnnotation(Args.class));
            System.out.println("+++++++++++ LAST ANNO: " + object.lastClassAnnotation(Args.class));
            
            System.out.println("++++++++++param count: " + object.parameterCount());
            
            //return 123;
            throw new RuntimeException("Got a FAILURE");
        }
    }
        
    class LocalResponseHandler extends AbstractResponseHandler<Integer, Object> {
        @Override
        public Object apply(ResponseWrapper<Integer, Object> object) {
            
            
            System.out.println("-------FOUND: " + object.value());
            System.out.println("Expected returnType: " + object.type());
            return object.value();
        }
    }
        
    
    class LocalErrorHandler extends AbstractErrorHandler<Integer> {
        @Override
        public Throwable apply(ErrorWrapper<Integer> object) {
            
            System.out.println("*************Hello local error: " + object.context());
            System.out.println("*************Hello local error: " + object.thrownException().getMessage());
            return new UnsupportedOperationException("Not supported yet: " + object.context()); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
        
    class LocalFallbackHandler extends AbstractFallbackHandler<Object> {
        @Override
        public Object apply(FallbackWrapper object) {
            
            System.out.println("Exepected return-type: " + object.returnType());
            System.out.println("Thrown exception: " + object.exception().getClass() + " message: " + object.exception().getMessage());
            System.out.println("Falling back to null");
            return 998877;
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
    static interface HelloWorld extends Bears {
        
        @Args( { "{message}" } )
        @ResponseHandler(LocalResponseHandler.class)
        @ExecutionHandler(LocalExecutionHandler.class)
        @RequestHandler(LocalRequestHandler.class)
        @ErrorHandler(LocalErrorHandler.class)
        @FallbackHandler(LocalFallbackHandler.class)
        Object helloWorld(@Nullable @ArgsValue("message") String message, int number, String monkey);
        
        @Args( { "{message}" } )
        //@ResponseHandler(LocalResponseHandler.class)
        @ExecutionHandler(LocalExecutionHandler.class)
        //@RequestHandler(LocalRequestHandler.class)
        @ErrorHandler(LocalErrorHandler.class)
        @FallbackHandler(LocalFallbackHandler.class)
        int helloWorld(@Nullable @ArgsValue("message") String message, String monkey, int number);

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
        /*
        String returnValue = helloWorld.helloWorld("fish", 123, null);

        System.out.println("-----> ReturnValue=" + returnValue);
        System.out.println("----->Ending...");
*/
        //helloWorld.helloWorld("fish", 123, null);
        //helloWorld.helloWorld("fish", 123, null);

        System.out.println(">>>>>> " + helloWorld.helloWorld("bear", null, 123));
        //System.out.println("output=" + helloWorld.helloWorld("bear", null, 123));
        //System.out.println("output=" + helloWorld.helloWorld("tiger", null, 123));
    }
}


