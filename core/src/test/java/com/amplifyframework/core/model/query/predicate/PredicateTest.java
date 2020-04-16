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

package com.amplifyframework.core.model.query.predicate;

import com.amplifyframework.testmodels.personcar.Person;

import org.junit.Test;

import java.util.Arrays;

import static com.amplifyframework.core.model.query.predicate.QueryPredicateOperation.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests predicate creation.
 */
public final class PredicateTest {
    /**
     * Tests the creation of a simple single operation predicate.
     */
    @Test
    public void testSingleQueryPredicateOperation() {
        QueryPredicateOperation<?> op = Person.ID.eq("1234");

        assertEquals("id", op.field());
        assertTrue(op.operator() instanceof EqualQueryOperator);
        assertEquals("1234", ((EqualQueryOperator) op.operator()).value());
    }

    /**
     * Tests the creation of an AND group predicate.
     */
    @Test
    public void testSingleQueryPredicateGroup() {
        QueryPredicateGroup op = Person.ID.eq("1234").and(Person.AGE.gt(21));

        QueryPredicateGroup expected = new QueryPredicateGroup(
                QueryPredicateGroup.Type.AND,
                Arrays.asList(
                        new QueryPredicateOperation<>("id", new EqualQueryOperator("1234")),
                        new QueryPredicateOperation<>("age", new GreaterThanQueryOperator<>(21))
                ));

        assertEquals(expected, op);
    }

    /**
     * Tests the evaluation of a predicate operation.
     */
    @Test
    public void testPredicateOperationEvaluation() {
        final Person jane = Person.builder()
                .firstName("Jane")
                .lastName("Doe")
                .age(21)
                .build();

        assertTrue(Person.AGE.gt(20).evaluate(jane));
        assertTrue(Person.AGE.eq(21).evaluate(jane));
        assertTrue(Person.AGE.lt(22).evaluate(jane));
        assertFalse(Person.AGE.ne(21).evaluate(jane));

        assertTrue(Person.FIRST_NAME.eq("Jane").evaluate(jane));
        assertTrue(Person.FIRST_NAME.beginsWith("J").evaluate(jane));
        assertTrue(Person.FIRST_NAME.contains("Jan").evaluate(jane));
        assertFalse(Person.LAST_NAME.eq("Jane").evaluate(jane));
    }

    /**
     * Tests the evaluation of a predicate group.
     */
    @Test
    public void testPredicateGroupEvaluation() {
        final Person jane = Person.builder()
                .firstName("Jane")
                .lastName("Doe")
                .age(21)
                .build();

        // True AND True = True
        assertTrue(Person.AGE.eq(21)
                .and(Person.FIRST_NAME.eq("Jane"))
                .evaluate(jane));
        // True AND False = False
        assertFalse(Person.AGE.eq(21)
                .and(Person.LAST_NAME.eq("Jane"))
                .evaluate(jane));
        // True OR False = True
        assertTrue(Person.AGE.eq(21)
                .or(Person.LAST_NAME.eq("Jane"))
                .evaluate(jane));
        // False OR False = False
        assertFalse(Person.AGE.gt(121)
                .or(Person.LAST_NAME.eq("Jane"))
                .evaluate(jane));
        // NOT(True) = False
        assertFalse(not(Person.AGE.eq(21))
                .evaluate(jane));
    }
}

