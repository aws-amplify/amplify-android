/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.network;

import com.amplifyframework.datastore.model.Model;

import java.util.Objects;
import java.util.UUID;

/**
 * Some little POJO we can use to test the queue.
 */
final class Person implements Model {
    private final String name;
    private final String uuid;

    private Person(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID().toString();
    }

    static Person named(final String name) {
        return new Person(Objects.requireNonNull(name));
    }

    String name() {
        return name;
    }

    @Override
    public String getId() {
        return uuid;
    }
}
