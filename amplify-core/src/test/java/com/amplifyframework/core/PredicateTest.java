/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.core;

import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import org.junit.Test;

public class PredicateTest {
    @Test
    public void testSingleQueryPredicateOperation() {
        QueryPredicateOperation op = field("id").eq("1234");

        assert(op.field().equals("id"));
        assert(op.operator().getClass().equals(EqualQueryOperator.class));
        assert(((EqualQueryOperator)op.operator()).value().equals("1234"));
    }
}
