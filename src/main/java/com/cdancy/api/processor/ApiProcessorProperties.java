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

package com.cdancy.api.processor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import java.util.Properties;

/**
 * Global properties to be used by ApiProcessor and any implementing frameworks. 
 * This class can be injected in the normal guice fashion.
 * 
 * @author cdancy
 */
@Singleton
public class ApiProcessorProperties {
    
    public final ImmutableMap<String, String> properties;
    
    /**
     * Create instance from default set of properties.
     * 
     * @param defaults init instance from passed property defaults.
     */
    public ApiProcessorProperties(Properties defaults) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        defaults.forEach((key, value) -> {
            String possibleKey = "" + checkNotNull(key, "property key cannot be null");
            String possibleValue = "" + checkNotNull(value, "property value cannot be null");
            builder.put(possibleKey, possibleValue);
        });
        this.properties = builder.build();
    }
    
    /**
     * Get a value from key, and if not present, return defaultValue.
     * 
     * @param key the key used to query for value.
     * @param defaultValue the value to use should query for key return null.
     * @return the queried value (possibly null).
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null) ? value : defaultValue;
    }
    
    /**
     * Get a value from key.
     * 
     * @param key the key used to query for value.
     * @return the queried value (possibly null).
     */
    public String get(String key) {
        String possibleValue = properties.get(key);
        if (possibleValue == null) {
            possibleValue = System.getProperty(key);
            if (possibleValue == null) {
                possibleValue = System.getenv(key);
            }
        }
        
        return possibleValue;
    }
}
