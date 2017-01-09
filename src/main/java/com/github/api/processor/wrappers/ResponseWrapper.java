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

package com.github.api.processor.wrappers;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nullable;

/**
 *
 * @author github.
 * @param <T>
 * @param <V>
 */
public class ResponseWrapper<T, V> {
    
    private final T value;
    private final V context;
    private final TypeToken type;
    
    public ResponseWrapper(@Nullable T value, @Nullable V context, TypeToken type) {
        this.value = value;
        this.context = context;
        this.type = type;
    }
    
    public T value() {
        return value;
    }
    
    public V context() {
        return context;
    }
    
    public TypeToken type() {
        return type;
    }
    
    public static <T, V> ResponseWrapper<T, V> newInstance(T value, V context, TypeToken type) {
        return new ResponseWrapper(value, context, type);
    }
}
