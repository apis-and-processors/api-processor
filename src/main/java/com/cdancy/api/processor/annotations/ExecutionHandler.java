/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.annotations;

import com.cdancy.api.processor.instance.InvocationInstance;
import com.google.common.base.Function;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * @author cdancy
 */
@Target( { TYPE, METHOD } )
@Retention( RUNTIME )
public @interface ExecutionHandler {
   Class<? extends Function<InvocationInstance, ?>> value();
}
