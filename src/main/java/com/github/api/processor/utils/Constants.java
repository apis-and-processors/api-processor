/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.utils;

/**
 *
 * @author dancc
 */
public class Constants {
    
    public static final String OBJECT_CLASS = "java.lang.Object";

    public static final String GREATER_THAN = "<";
    public static final String LESS_THAN = ">";
    public static final String COMMA_SPACE = ", ";
    
    public static final char GREATER_THAN_CHAR = '<';
    public static final char LESS_THAN_CHAR = '>';
    public static final char COMMA_CHAR = ',';
    public static final char SPACE_CHAR = ' ';   
    
    
    public static final int REQUEST_HANDLER_TO_EXECUTION_HANDLER_CHECK = 0;
    public static final int EXECUTION_HANDLER_TO_ERROR_HANDLER_CHECK = 1;
    public static final int FALLBACK_HANDLER_TO_RETURN_VALUE_CHECK = 2;
    public static final int EXECUTION_HANDLER_TO_RESPONSE_HANDLER_CHECK = 3;
    public static final int RESPONSE_HANDLER_TO_RETURN_VALUE_CHECK = 4;
    public static final int EXECUTION_HANDLER_TO_RETURN_VALUE_CHECK = 5;

    private Constants() {
        throw new UnsupportedOperationException("Purposely not implemented");
    }
}
