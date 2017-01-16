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
public class ParsedType implements Comparable<ParsedType> {

    private final String mainType;
    private final ParsedType parentType;
    private final List<ParsedType> subTypes = Lists.newArrayList();

    public ParsedType(String mainType, ParsedType parentType) {
        this.mainType = mainType;
        this.parentType = parentType;
    }  

    public ParsedType add(ParsedType genericTypes) {
        if (genericTypes != null) {
            subTypes.add(genericTypes);
        }
        return this;
    }
    
    public String mainType() {
        return mainType;
    }
       
    public ParsedType parentType() {
        return parentType;
    }
        
    public List<ParsedType> subTypes() {
        return subTypes;    
    }
    
    public ParsedType subTypeAtIndex(int index) {
        return subTypes.get(index);
    }
    
    public int compare(ParsedType compareTo) {
        return compare(this, compareTo);
    }
    
    @Override
    public int compareTo(ParsedType compareTo) {
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
    private static int compare(ParsedType source, ParsedType target) {      
        if(source.mainType.equals(target.mainType)) {            
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
                    + source.mainType + "' has " + sourceSize + " subTypes while '" 
                    + target.mainType + "' has " + targetSize + " subTypes", 
                        source.mainType, target.mainType); 
            }
        } else {
            if (isTypeUnknown(source.mainType)) {
                return 1;
            } else if(isTypeUnknown(target.mainType)) {
                return 2;
            } else {
                throw new TypeMismatchException("Source type '" 
                    + source.mainType + "' does not match target type '" 
                    + target.mainType + "'", 
                        source.mainType, target.mainType);
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
    
    private static void print(ParsedType genericTypes, StringBuilder builder) {
        builder.append(genericTypes.mainType);
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
