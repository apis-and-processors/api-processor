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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    
    class LocalErrorHandler extends AbstractErrorHandler<String> {
        @Override
        public Throwable apply(ErrorWrapper<String> object) {
            
            System.out.println("*************Hello local error: " + object.context());
            return new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
        
    class LocalExecutionHandler extends AbstractExecutionHandler<SpecialBean, String> {
        @Override
        public String apply(InvocationInstance<SpecialBean> object) {
            
            System.out.println("Context: " + object.context());
            System.out.println("properties: " + object.context().props());
            object.context().properties.put("fish", "bear");
            
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
            
            //return "monkey";
            throw new RuntimeException("Got a FAILURE");
        }
    }
        
    class LocalFallbackHandler extends AbstractFallbackHandler<String> {
        @Override
        public String apply(FallbackWrapper object) {
            
            System.out.println("Exepected return-type: " + object.returnType);
            System.out.println("Thrown exception: " + object.thrownException.getClass() + " message: " + object.thrownException.getMessage());
            System.out.println("Falling back to null");
            return "fish";
        }
    }
            
    class LocalRequestHandler extends AbstractRequestHandler<SpecialBean> {
        @Override
        public SpecialBean apply(SpecialBean object) {
            System.out.println("______________REQUESTHANDLER: " + object.getClass());
            return new SpecialBean();
        }
    }
        
    class LocalResponseHandler extends AbstractResponseHandler<SpecialBean, Object> {
        @Override
        public Object apply(ResponseWrapper<SpecialBean> object) {
            System.out.println("@@@@@@@@@@@@ INSIDE @@@@@@@@@@@@@22");
            return object.returnValue;
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
        String returnValue = helloWorld.helloWorld("fish", 123, null);

        System.out.println("-----> ReturnValue=" + returnValue);
        System.out.println("----->Ending...");
        
    }
}


