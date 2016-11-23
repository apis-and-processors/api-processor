/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdancy.api.processor.handlers;

import com.cdancy.api.processor.wrappers.ErrorWrapper;
import com.google.common.base.Function;

/**
 *
 * @author cdancy
 */
public abstract class AbstractErrorHandler implements Function<ErrorWrapper, Throwable> {

}
