package com.cdancy.api.processor;

import com.cdancy.api.processor.annotations.Api;
import com.cdancy.api.processor.annotations.Args;
import com.cdancy.api.processor.annotations.ArgsValue;
import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.testng.annotations.Test;

public class ApiProcessorTest {
    
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
        Optional<String> helloWorld(@Nullable @ArgsValue("message") String message, int number, String monkey);
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


