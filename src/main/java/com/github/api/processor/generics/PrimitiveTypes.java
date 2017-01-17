/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.generics;

import com.github.api.processor.utils.Constants;

/**
 *
 * @author dancc
 */
public enum PrimitiveTypes {
    
    BYTE("byte", Byte.class),
    CHAR("char", Character.class),
    SHORT("short", Short.class),
    INT("int", Integer.class),
    LONG("long", Long.class),
    FLOAT("float", Float.class),
    DOUBLE("double", Double.class),
    BOOLEAN("boolean", Boolean.class),
    VOID("void", Void.class),
    NULL("null", Null.class);

    private final String name;
    private final Class rawClass;
    
    private PrimitiveTypes(String name, Class rawClass) {
        this.name = name;
        this.rawClass = rawClass;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Class getRawClass() {
        return this.rawClass;
    }

    public static PrimitiveTypes fromName(Object name) {
        return name == null ? PrimitiveTypes.NULL : fromName(name.toString());
    }
        
    public static PrimitiveTypes fromName(String name) {
        return (name == null || name.trim().equalsIgnoreCase(Constants.NULL_STRING)) 
                ? PrimitiveTypes.NULL 
                : PrimitiveTypes.valueOf(name.toUpperCase().intern());
    }

}