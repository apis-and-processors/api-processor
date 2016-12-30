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

package com.cdancy.api.processor.wrappers;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nullable;

/**
 *
 * @author cdancy.
 */
public class ResponseWrapper<T> {
    
    public final T returnValue;
    public final TypeToken returnType;
    
    public ResponseWrapper(@Nullable T returnValue, TypeToken returnType) {
        this.returnValue = returnValue;
        this.returnType = returnType;
    }
    
    public Object returnValue() {
        return returnValue;
    }
    
    public TypeToken returnType() {
        return returnType;
    }
    
    public static <T> ResponseWrapper<T> newInstance(T returnValue, TypeToken returnType) {
        return new ResponseWrapper(returnValue, returnType);
    }
}
