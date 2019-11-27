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

package com.amplifyframework.api.aws;

import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.testmodels.Person;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Tests predicate creation.
 */
public final class PredicateTest {
    /**
     * Tests the creation of a simple single operation predicate.
     */
    @Test
    public void testSingleQueryPredicateOperation() {
        QueryPredicateOperation op = Person.ID.eq("1234");

        assert (op.field().equals("id"));
        assert (op.operator().getClass().equals(EqualQueryOperator.class));
        assert (((EqualQueryOperator) op.operator()).value().equals("1234"));
    }

    /**
     * Tests the creation of an AND group predicate.
     */
    @Test
    @SuppressWarnings("magicnumber")
    public void testSingleQueryPredicateGroup() {
        QueryPredicateGroup op = Person.ID.eq("1234").and(Person.AGE.gt(21));

        QueryPredicateGroup expected = new QueryPredicateGroup(
                QueryPredicateGroup.Type.AND,
                Arrays.asList(
                        new QueryPredicateOperation("id", new EqualQueryOperator("1234")),
                        new QueryPredicateOperation("age", new GreaterThanQueryOperator(21))
                ));

        Assert.assertEquals(expected, op);
    }
}

