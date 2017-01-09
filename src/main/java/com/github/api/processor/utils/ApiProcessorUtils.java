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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.base.Throwables;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
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
            return beanClass.cast(noArgConstructor.newInstance(EMPTY_OBJECT_ARRAY)); 
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException | IllegalArgumentException ex) {
            
            // second attempt at creating generic object from class
            try {
                return beanClass.newInstance();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } 
    }   
    
    public Class[] getGenericClassTypes(Class clazz) {
        TypeToken.TypeSet genericType = TypeToken.of(clazz).getTypes().classes();
        Iterator<TypeToken> iter = genericType.iterator();
        String clazzName = clazz.getSuperclass().getCanonicalName();
        while(iter.hasNext()) {
            TypeToken typeToken = iter.next();
            String typeTokenClassName = typeToken.toString();
            if (typeTokenClassName.startsWith(clazzName)) {
               String[] typeStrings = typeTokenClassName
                       .substring(clazzName.length(), typeTokenClassName.length())
                       .replaceFirst("<", "")
                       .replaceFirst(">", "")
                       .replaceAll(" ", "")
                       .split(",");
               Class[] typeClasses = new Class[typeStrings.length];
               for (int i = 0; i < typeClasses.length; i++) {
                   try {
                       typeClasses[i] = Class.forName(typeStrings[i]);
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
            ? Primitive.fromName(potentialPrimitive.toGenericString()).getRawClass()
            : potentialPrimitive;
    }
}
