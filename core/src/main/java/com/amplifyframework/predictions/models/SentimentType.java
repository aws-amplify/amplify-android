/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.predictions.models;

/**
 * List of types of sentiments that can be associated
 * with text.
 */
public enum SentimentType {
    /**
     * Exhibits positive sentiments.
     */
    POSITIVE,

    /**
     * Exhibits negative sentiments.
     */
    NEGATIVE,

    /**
     * Exhibits neither positive nor negative sentiments.
     */
    NEUTRAL,

    /**
     * Exhibits both positive and negative sentiments.
     */
    MIXED,

    /**
     * For any sentiment that doesn't fall into the other categories.
     */
    UNKNOWN
}
