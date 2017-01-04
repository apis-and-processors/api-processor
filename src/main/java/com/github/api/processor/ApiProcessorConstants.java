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

package com.github.api.processor;

/**
 * Constants used in api-processor services.
 */
public final class ApiProcessorConstants {

    /**
     * Long property.
     * 
     * <p>Expire entries in cache after the specified time (in milliseconds) passed since last access. Default is 360000.
     */
    public static final String CACHE_EXPIRE = "api-processor.cache-expire";
    
    /**
     * Integer property.
     * 
     * <p>Number of retries an ExecutionHandler will be re-run upon failure. Default is 0.
     */
    public static final String RETRY_COUNT = "api-processor.retry-count";
    public static final String RETRY_COUNT_DEFAULT = "0";

    /**
     * Long property.
     * 
     * <p>Time (in milliseconds) between ExecutionHandler retries if {@link #RETRY_COUNT} is greater
     * than zero.
     */
    public static final String RETRY_DELAY_START = "api-processor.retry-delay-start";
    public static final String RETRY_DELAY_START_DEFAULT = "5000";

    /** 
     * Comma-separated list of methods considered idempotent for purposes of retries.  
     */
    public static final String IDEMPOTENT_METHODS = "api-processor.idempotent-methods";
   

    private ApiProcessorConstants() {
        throw new UnsupportedOperationException("intentionally unimplemented");
    }
}
