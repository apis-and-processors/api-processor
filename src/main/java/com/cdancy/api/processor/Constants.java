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


/**
 * Constants used in api-processor services.
 */
public final class Constants {

    /**
     * Integer property.
     * 
     * <p>Commands are retried, if the problem on the server side was a resolvable conflict. However,
     * the maximum tries of a single command is bounded.
     */
    public static final String RETRY_COUNT = "api-processor.retry-count";

    /**
     * Long property.
     * 
     * <p>Commands are retried, if the problem on the server side was a resolvable conflict. However,
     * the maximum tries of a single command is bounded. If {@link #RETRY_COUNT} is greater
     * than zero, this property is used to determine the start delay. The delay is based on exponential
     * backoff algorithm. Default value for this property is 50 milliseconds.
     */
    public static final String RETRY_DELAY_START = "api-processor.retry-delay-start";


    /** Comma-separated list of methods considered idempotent for purposes of retries.  */
    public static final String PROPERTY_IDEMPOTENT_METHODS = "api-processor.idempotent-methods";
   

    private Constants() {
        throw new AssertionError("intentionally unimplemented");
    }
}
