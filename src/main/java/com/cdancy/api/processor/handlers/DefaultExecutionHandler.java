/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.instance.InvocationInstance;
import com.google.inject.Singleton;

/**
 *
 * @author cdancy
 */
@Singleton
public class DefaultExecutionHandler extends AbstractExecutionHandler {

    @Override
    public Object apply(InvocationInstance f) {
        return null;
    }
}
