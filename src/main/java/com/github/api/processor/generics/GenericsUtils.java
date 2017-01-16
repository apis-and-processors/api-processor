/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.generics;

import com.github.api.processor.utils.Constants;
import com.github.api.processor.handlers.AbstractExecutionHandler;
import com.github.api.processor.handlers.AbstractRequestHandler;
import com.github.api.processor.instance.InvocationInstance;
import com.google.common.base.Throwables;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dancc
 */
public class GenericsUtils {
   
    public static final Field[] VALUE_FIELD = new Field[1];
    static {
        try {
            VALUE_FIELD[0] = String.class.getDeclaredField("value");
            VALUE_FIELD[0].setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            // ignore as we know the field exists
        }
    }
    
    private static class LocalExecutionHandler extends AbstractExecutionHandler<String, Map<String, List<Integer>>> {

        @Override
        public Map<String, List<Integer>> apply(InvocationInstance<String> t) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    private static class LocalRequestHandler2 extends AbstractRequestHandler<String, Map<String, List<Integer>>> {
        @Override
        public Map<String, List<Integer>> apply(String object) {
            return null;
        }
    }
    
    public static void main(String [] args) {
        
        AbstractExecutionHandler handler = new LocalExecutionHandler();
        AbstractRequestHandler handler2 = new LocalRequestHandler2();

        //String tester = "com.github.api.processor.handlers.AbstractResponseHandler<java.lang.Integer, java.util.Map<java.lang.String, java.lang.Object>, java.lang.Boolean, java.util.Map<java.lang.String, java.lang.List<java.lang.Void>>>";
        //String tester = "com.handlers.AbstractResponseHandler<java.lang.Map<java.lang.String, java.lang.Integer>, java.lang.Boolean>";
        String tester = "com.handlers.AbstractResponseHandler<java.lang.Integer<String, Integer, Void, Bear>, java.lang.Void<java.lang.Char>, java.lang.Map<java.lang.Char, java.lang.List<java.lang.Boolean>>>";

        // System.out.println("+++OUTPUT-1: " + tester);

        //GenericTypes genericTypes = getGenericTypesAsStrings(tester, null, null);
        ParsedType genericTypes = parseType(handler);
        ParsedType genericTypes2 = parseType(handler2);
        
        System.out.println("+++OUTPUT-1: " + genericTypes);
        System.out.println("+++OUTPUT-2: " + genericTypes2);
        
        //int num = genericTypes.compareTo(genericTypes2, true);
        
        //System.out.println("+++OUTPUT-3: " + num);
        System.out.println("+++OUTPUT-3: " + genericTypes.subTypeAtIndex(1).compare(genericTypes2.subTypeAtIndex(1)));
        System.out.println("index-0: " + genericTypes.subTypeAtIndex(1));
        System.out.println("index-0: " + genericTypes2.subTypeAtIndex(1));

        /*
        while(genericTypes != null) {
            System.out.println("main: " + genericTypes.mainType);
            if (genericTypes.subTypes.size() > 0) {
                genericTypes = genericTypes.subTypes.get(0);
            } else {
                genericTypes = null;
            }
        }
        */
        
        //System.out.println(genericTypes);
        
    }
    
    public static ParsedType parseType(Object clazz) {
        return parseType(clazz == null ? 
                PrimitiveTypes.fromName(clazz).getRawClass() : 
                clazz.getClass());
    }

    public static ParsedType parseType(Class clazz) {
        if (clazz.getGenericSuperclass() != null) {

            // check if generic string already returns type info: public class java.util.ArrayList<E>
            boolean isAlreadyGeneric = clazz.toGenericString().matches("^(public|private|protected) .+");
            if (isAlreadyGeneric) {
                String [] parts = clazz.toGenericString().split(" ");
                return parseType(parts[parts.length - 1], null, null);
            } else {
                return parseType(clazz.getGenericSuperclass());
            }
        } else {
            String [] parts = clazz.toGenericString().split(" ");
            return parseType(parts[parts.length - 1], null, null);
        }
    }
    
    public static ParsedType parseType(Type type) {
        return parseType(type.getTypeName());
    }
        
    public static ParsedType parseType(String clazz) {
        return parseType(clazz, null, null);
    }
        
    private static ParsedType parseType(String clazzAndTypes, ParsedType genericTypes, StringBuilder builder) {

        int index = clazzAndTypes.indexOf(Constants.GREATER_THAN);
        if (index == -1) {
            if (genericTypes != null) {
                return new ParsedType(clazzAndTypes, genericTypes);
            } else {
                return new ParsedType(clazzAndTypes, null);
            }
        }
        
        ParsedType types = new ParsedType(clazzAndTypes.substring(0, index), (genericTypes != null ? genericTypes : null));
        try {
                        
            if (builder == null) {
                builder = new StringBuilder();
            }
                        
            char[] chars = (char[]) VALUE_FIELD[0].get(clazzAndTypes);
            int stopPoint = chars.length - 2;
            int lessThanEncountered = 0;
            for (int i = index + 1; i < chars.length -1; i++) {
                
                if (chars[i] != Constants.SPACE_CHAR) {
                    builder.append(chars[i]);
                    
                    switch (chars[i]) {
                        case Constants.GREATER_THAN_CHAR:
                            lessThanEncountered += 1;
                            break;
                        case Constants.LESS_THAN_CHAR:
                            lessThanEncountered -= 1;
                            if (i == stopPoint) {
                                String foundType = builder.toString();  
                                builder.setLength(0);
                                ParsedType type = parseType(foundType, types, builder);
                                types.add(type);
                            }   
                            break;
                        case Constants.COMMA_CHAR:
                            if (lessThanEncountered == 0) {
                                builder.deleteCharAt(builder.length() - 1);
                                String foundType = builder.toString();
                                builder.setLength(0);
                                ParsedType type = parseType(foundType, types, builder);
                                types.add(type);                                
                            } 
                            break;
                        default:
                            if (i == stopPoint) {
                                String foundType = builder.toString();
                                builder.setLength(0);
                                ParsedType type = parseType(foundType, types, builder);
                                types.add(type);  
                            } 
                            break;
                    }
                }                
            }            
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw Throwables.propagate(ex);
        }

        return types;  
    }
    
    private GenericsUtils() {
        throw new UnsupportedOperationException("Purposely not implemented");
    }
}
