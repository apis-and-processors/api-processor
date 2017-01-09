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
public enum Primitive {
    
    BYTE("byte", Byte.class),
    CHAR("char", Character.class),
    SHORT("short", Short.class),
    INT("int", Integer.class),
    LONG("long", Long.class),
    FLOAT("float", Float.class),
    DOUBLE("double", Double.class),
    BOOLEAN("boolean", Boolean.class),
    VOID("void", Void.class);

    private final String name;
    private final Class rawClass;
    
    private Primitive(String name, Class rawClass) {
        this.name = name;
        this.rawClass = rawClass;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Class getRawClass() {
        return this.rawClass;
    }

    public static Primitive fromName(String name) {
        return name != null ? Primitive.valueOf(name.toUpperCase().intern()) : null;
    }

}