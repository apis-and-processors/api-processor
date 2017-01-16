/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.api.processor.utils;

import com.github.api.processor.generics.PrimitiveTypes;
import com.github.api.processor.generics.ParsedType;
import com.github.api.processor.handlers.AbstractRequestHandler;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author github.
 */
@Singleton
public class ApiProcessorUtils {

    private static final String CLASS_ANNO_NULL = "class annotataion cannot be null";
    private static final String CLASS_ANNO_REQUIRED = "class must be an annotation";
    private static final String POTENTIAL_PRIMITIVE_NULL = "potentialPrimitive class cannot be null";

    private static final Object [] EMPTY_OBJECT_ARRAY = new Object[1];
    private static final Constructor OBJECT_CONSTRUCTOR = Object.class.getDeclaredConstructors()[0];
    
    /**
     * Find all classes on the path annotated with given annotation.
     * 
     * @param annotation the annotation to scan for.
     * @return set of annotated classes.
     */
    public Set<Class> findClassesAnnotatedWith(Class annotation) {
        checkNotNull(annotation, CLASS_ANNO_NULL);
        checkArgument(annotation.isAnnotation(), CLASS_ANNO_REQUIRED);
        Set<Class> builtApis = Sets.newHashSet();
        new FastClasspathScanner()
                .matchClassesWithAnnotation(annotation, c -> {
                    builtApis.add(c);
                }).scan();
        return builtApis;
    }
    
    public <T> T newInterfaceInstance(Class<T> beanInterface) {   
        try {
            Constructor genericConstructor = sun.reflect.ReflectionFactory
                    .getReflectionFactory()
                    .newConstructorForSerialization(beanInterface, OBJECT_CONSTRUCTOR);

            return beanInterface.cast(genericConstructor.newInstance()); 
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException | IllegalArgumentException ex) {
            throw Throwables.propagate(ex);
        } 
    }     
    
    public <T> T newClassInstance(Class<T> beanClass) {   
        try {
            Constructor noArgConstructor = beanClass.getDeclaredConstructors()[0];
            noArgConstructor.setAccessible(true);
            
            if (Number.class.isAssignableFrom(beanClass)) {
                return beanClass.cast(noArgConstructor.newInstance(0));     
            } else {
                return beanClass.cast(noArgConstructor.newInstance(EMPTY_OBJECT_ARRAY));     
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException | IllegalArgumentException ex) {
            
            // second attempt at creating generic object from class
            try {
                return beanClass.newInstance();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } 
    }   
        
    public Class[] getGenericTypesAsClasses(Class clazz) {
        TypeToken.TypeSet genericType = TypeToken.of(clazz).getTypes();
        Iterator<TypeToken> iter = genericType.iterator();
        String clazzName = clazz.getSuperclass().getCanonicalName();
        while(iter.hasNext()) {
            String typeTokenClassName = iter.next().toString();
            if (typeTokenClassName.startsWith(clazzName)) {
                typeTokenClassName = typeTokenClassName.substring(clazzName.length(), typeTokenClassName.length());
                String[] classStrings = classStringsFromTypeStrings(typeTokenClassName);
                Class[] typeClasses = new Class[classStrings.length];
                for (int i = 0; i < typeClasses.length; i++) {
                    try {
                        typeClasses[i] = Class.forName(classStrings[i]);
                    } catch (ClassNotFoundException ex) {
                        throw Throwables.propagate(ex);
                    }
                }
                return typeClasses;
            }
        }
        
        return new Class[0];
    }

    public Class potentialPrimitiveToClass(Class potentialPrimitive) {
        checkNotNull(potentialPrimitive, POTENTIAL_PRIMITIVE_NULL);
        return potentialPrimitive.isPrimitive() 
            ? PrimitiveTypes.fromName(potentialPrimitive.toGenericString()).getRawClass()
            : potentialPrimitive;
    }
    
    /**
     * Converts a String that looks like:
     * 
     *     <java.util.ArrayList<java.lang.String>, java.util.HashMap<java.lang.String, java.lang.String>>
     * 
     * to a list that looks like:
     * 
     *     { 'java.util.ArrayList', 'java.util.HashMap' }
     * 
     * @param typeString string containing possible types.
     * @return string array with all types removed.
     */
    private String[] classStringsFromTypeStrings(String typeString) {
        int index = typeString.lastIndexOf("<");
        if (index != -1) {
            String firstPass = typeString.replaceFirst("<", "").replaceAll(" ", "");
            String[] classStrings = replaceLastString(firstPass, ">", "").split(",");
            for(int i = 0; i < classStrings.length; i++) {
                classStrings[i] = removeTypeStringFromClassString(classStrings[i]);
            }
            return classStrings;
        } else {
            String [] possibleTypeStrings = { typeString };
            return possibleTypeStrings;
        }
    }
    
    /**
     * Converts a String that looks like:
     *     
     *     'java.util.ArrayList<String, List<String>>' 
     * 
     * to one that looks like:
     * 
     *     'java.util.ArrayList'.
     * 
     * If no types are found than original String is returned.
     * 
     * @param classString string containing potential types.
     * @return the class portion of the type-string or original string
     *         if none could be found.
     */
    public String removeTypeStringFromClassString(String classString) {
        int index = classString.indexOf("<");
        return (index != -1) 
                ? classString.substring(0, index)
                : classString;
    }
    
    /**
     * Replace the last occurrence of a String within a given String
     * 
     * @param source the source string we will work on.
     * @param substring string we will replace the last occurrence of.
     * @param replacement string we will use to replace last occurrence of 'substring'.
     * @return new String with all replacements done or original String if substring was not found.
     */
    public String replaceLastString(String source, String substring, String replacement) {
        int index = source.lastIndexOf(substring);
        return (index == -1) 
                ? source 
                : source.substring(0, index) 
                + replacement + source.substring(index + substring.length());
    }
}
