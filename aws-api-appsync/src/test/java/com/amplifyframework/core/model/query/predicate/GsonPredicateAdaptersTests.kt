/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.core.model.query.predicate

import com.amplifyframework.testmodels.todo.Todo
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class GsonPredicateAdaptersTests {

    private val gson = Gson().newBuilder().apply {
        GsonPredicateAdapters.register(this)
    }.create()

    @Test
    fun `serialize and deserialize single predicate`() {
        val queryPredicate = Todo.ID.eq("123")
        val queryPredicateString = gson.toJson(queryPredicate)
        val expectedString = """
            {"field":"id","operator":{"value":"123","type":"EQUAL"},"_type":"OPERATION"}
        """.replace("\\s".toRegex(), "")
        assertEquals(expectedString, queryPredicateString)
        val deserializedPredicate = gson.fromJson(queryPredicateString, QueryPredicate::class.java)
        assertEquals(queryPredicate, deserializedPredicate)
    }

    @Test
    fun `serialize and deserialize group predicate`() {
        val queryPredicate = Todo.TITLE.eq("Title").and(Todo.ID.eq("123"))
        val queryPredicateString = gson.toJson(queryPredicate)
        val expectedString = """
            {
                "type":"AND",
                "predicates":[
                    {"field":"title","operator":{"value":"Title","type":"EQUAL"},"_type":"OPERATION"},
                    {"field":"id","operator":{"value":"123","type":"EQUAL"},"_type":"OPERATION"}
                ],
                "_type":"GROUP"
            }
        """.replace("\\s".toRegex(), "")
        assertEquals(expectedString, queryPredicateString)
        val deserializedPredicate = gson.fromJson(queryPredicateString, QueryPredicate::class.java)
        assertEquals(queryPredicate, deserializedPredicate)
    }

    @Test
    fun `serialize and deserialize nested group predicates`() {
        val queryPredicate = Todo.TITLE.eq("Title")
            .and(Todo.ID.eq("123").or(Todo.ID.eq("456")))
        val queryPredicateString = gson.toJson(queryPredicate)
        val expectedString = """
            {
                "type": "AND",
                "predicates": [
                    {
                        "field": "title",
                        "operator": {
                            "value": "Title",
                            "type": "EQUAL"
                        },
                        "_type": "OPERATION"
                    },
                    {
                        "type": "OR",
                        "predicates": [
                            {
                                "field": "id",
                                "operator": {
                                    "value": "123",
                                    "type": "EQUAL"
                                },
                                "_type": "OPERATION"
                            },
                            {
                                "field": "id",
                                "operator": {
                                    "value": "456",
                                    "type": "EQUAL"
                                },
                                "_type": "OPERATION"
                            }
                        ],
                        "_type": "GROUP"
                    }
                ],
                "_type": "GROUP"
            }
        """.replace("\\s".toRegex(), "")
        assertEquals(expectedString, queryPredicateString)
        val deserializedPredicate = gson.fromJson(queryPredicateString, QueryPredicate::class.java)
        assertEquals(queryPredicate, deserializedPredicate)
    }
}