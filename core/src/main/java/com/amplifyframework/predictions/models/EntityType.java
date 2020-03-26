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
 * An entity can be assigned any of the following types.
 *
 * @see <a href=https://docs.aws.amazon.com/comprehend/latest/dg/how-entities.html>Detect Entities</a>
 */
@SuppressWarnings("JavadocVariable")
public enum EntityType {
    /**
     * A branded product.
     */
    COMMERCIAL_ITEM,

    /**
     * Indicates time. For example, a full date (11/25/2017),
     * day (Tuesday), month (May), or time (8:30 a.m.)
     */
    DATE,

    /**
     * An event, such as a festival, concert, election, etc.
     */
    EVENT,

    /**
     * A specific location, such as a country, city, lake,
     * building, etc.
     */
    LOCATION,

    /**
     * Large organizations, such as a government, company,
     * religion, sports team, etc.
     */
    ORGANIZATION,

    /**
     * Individuals, groups of people, nicknames, fictional
     * characters.
     */
    PERSON,

    /**
     * A quantified amount, such as currency, percentages,
     * numbers, bytes, etc.
     */
    QUANTITY,

    /**
     * An official name given to any creation or creative
     * work, such as movies, books, songs, etc.
     */
    TITLE,

    /**
     * Entities that don't fit into any of the other entity
     * categories.
     */
    UNKNOWN
}
