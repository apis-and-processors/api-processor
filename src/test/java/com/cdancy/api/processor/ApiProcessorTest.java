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
        public Throwable apply(ErrorWrapper f) {
            System.out.println("Hello local error");
            return new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
   
    class LocalExecutionHandler2 extends AbstractExecutionHandler {
        @Override
        public Object apply(InvocationInstance f) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return "fish and fries";
        }
    }
        
    class LocalExecutionHandler extends AbstractExecutionHandler {
        @Override
        public Object apply(InvocationInstance f) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return "Hoagies and Grinders";
        }
    }
        
    class LocalFallbackHandler extends AbstractFallbackHandler {
        @Override
        public Object apply(FallbackWrapper f) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
            
    class LocalResponseHandler extends AbstractResponseHandler {
        @Override
        public Object apply(ResponseWrapper f) {
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
    @ErrorHandler(LocalErrorHandler.class)
    @ExecutionHandler(LocalExecutionHandler2.class)
    static interface HelloWorld extends Bears {
        
        @Args( { "{message}" } )
        @ErrorHandler(LocalErrorHandler.class)
        @ExecutionHandler(LocalExecutionHandler.class)
        String helloWorld(@Nullable @ArgsValue("message") String message, int number, String monkey);
    }
    
    @Test
    public void testSomeLibraryMethod() {
        
        System.out.println("----->Starting...");
        
        HelloWorld helloWorld = ApiProcessor.builder().scanClasspath().build().get(HelloWorld.class);
        Object returnValue = helloWorld.helloWorld("fish", 123, null);
        Object returnValue1 = helloWorld.helloWorld("fish", 123, null);

        System.out.println("-----> ReturnValue=" + returnValue);
        System.out.println("----->Ending...");
        
        Set<Class> fish = new HashSet<>();
    }
}


