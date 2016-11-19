/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.google.inject.Singleton;

/**
 *
 * @author cdancy
 */
@Singleton
public class DefaultResponseHandler extends AbstractResponseHandler {

    @Override
    public Object apply(ResponseWrapper f) {
        System.out.println("ReturnedValue: " + f.returnValue);
        System.out.println("ExpectedType: " + f.expectedType);
        
        return null;
    }
}
