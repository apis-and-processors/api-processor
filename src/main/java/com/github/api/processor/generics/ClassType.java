/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.generics;

import com.github.api.processor.utils.Constants;
import com.github.api.processor.exceptions.TypeMismatchException;
import com.google.common.collect.Lists;
import java.util.List;

/**
 *
 * @author dancc
 */
public class ClassType implements Comparable<ClassType> {

    private final String name;
    private final ClassType parent;
    private final List<ClassType> subTypes = Lists.newArrayList();

    public ClassType(String name, ClassType parent) {
        this.name = name;
        this.parent = parent;
    }  

    public ClassType add(ClassType classType) {
        if (classType != null) {
            subTypes.add(classType);
        }
        return this;
    }
    
    public String name() {
        return name;
    }
       
    public ClassType parent() {
        return parent;
    }
        
    public List<ClassType> subTypes() {
        return subTypes;    
    }
    
    public ClassType subTypeAtIndex(int index) {
        return subTypes.get(index);
    }
    
    public int compare(ClassType compareTo) {
        return compare(this, compareTo);
    }
    
    @Override
    public int compareTo(ClassType compareTo) {
        try {
            return compare(this, compareTo);
        } catch (TypeMismatchException e) {
            return -1;
        }
    }
    
    /**
     * 1 == source has unknown types
     * 2 == target has unknown types
     * 3 == source and target have unknown types
     * 
     * @param source
     * @param target
     * @return 
     */
    private static int compare(ClassType source, ClassType target) {      
        if(source.name.equals(target.name)) {            
            int sourceSize = source.subTypes().size();
            int targetSize = target.subTypes().size();
            if (sourceSize == targetSize) {
                int counter = 0;
                for(int i = 0; i < sourceSize; i++) {
                    int localCount = compare(source.subTypes().get(i), target.subTypes().get(i));
                    switch(localCount) {
                        case 0:
                            break;
                        case 1:
                            if (counter == 0 || counter == 2) {
                                counter ++;
                            }
                            break;
                        case 2:
                            if (counter == 0 || counter == 1) {
                                counter ++;
                            }
                            break;
                    }
                }
                return counter;
            } else {
                throw new TypeMismatchException("Source type '" 
                    + source.name + "' has " + sourceSize + " subTypes while '" 
                    + target.name + "' has " + targetSize + " subTypes", 
                        source.name, target.name); 
            }
        } else {
            if (isTypeUnknown(source.name)) {
                return 1;
            } else if(isTypeUnknown(target.name)) {
                return 2;
            } else {
                throw new TypeMismatchException("Source type '" 
                    + source.name + "' does not match target type '" 
                    + target.name + "'", 
                        source.name, target.name);
            }
        }        
    }
    
    private static boolean isTypeUnknown(String possiblyUnknownType) {
        if (!possiblyUnknownType.equals(Constants.OBJECT_CLASS)) {
            try {
                GenericTypes.valueOf(possiblyUnknownType);                
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
    
    private static void print(ClassType genericTypes, StringBuilder builder) {
        builder.append(genericTypes.name);
        if (genericTypes.subTypes().size() > 0) {
            builder.append(Constants.GREATER_THAN);
            int size = genericTypes.subTypes().size();
            for(int i = 0; i < size; i++) {
                print(genericTypes.subTypes().get(i), builder);
                if (size > 0 && i != (size - 1)) {
                    builder.append(Constants.COMMA_SPACE);
                }
            }
            builder.append(Constants.LESS_THAN);
        }
    }
        
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        print (this, builder);
        return builder.toString();
    }
}
