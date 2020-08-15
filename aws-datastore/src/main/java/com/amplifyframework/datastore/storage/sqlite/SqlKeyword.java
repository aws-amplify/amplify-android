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

package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates SQL keywords in an enum with some
 * utility for type conversion.
 */
public enum SqlKeyword {
    /**
     * Acts as a delimiter between commands in SQL.
     */
    DELIMITER(" "),

    /**
     * SQL operator to check for equality.
     */
    EQUAL("="),

    /**
     * SQL operator to check for inequliaty.
     */
    NOT_EQUAL("!="),

    /**
     * SQL operator to check if a value is greater than another.
     */
    GREATER_THAN(">"),

    /**
     * SQL operator to check if a value is greater than or
     * equal to another.
     */
    GREATER_OR_EQUAL(">="),

    /**
     * SQL operator to check if a value is less than another.
     */
    LESS_THAN("<"),

    /**
     * SQL operator to check if a value is less than or
     * equal to another.
     */
    LESS_OR_EQUAL("<="),

    /**
     * SQL binary operator for AND.
     */
    AND("AND"),

    /**
     * SQL binary operator for OR.
     */
    OR("OR"),

    /**
     * SQL unary operator for NOT.
     */
    NOT("NOT"),

    /**
     * SQL keyword to check if a value is in range of values.
     */
    BETWEEN("BETWEEN"),

    /**
     * SQL keyword to check if a string is contained in another.
     */
    IN("IN"),

    /**
     * SQL keyword to check if a string fits provided pattern.
     */
    LIKE("LIKE"),

    /**
     * SQL keyword to begin a SELECT statement.
     */
    SELECT("SELECT"),

    /**
     * SQL keyword to specify tables to operate on.
     */
    FROM("FROM"),

    /**
     * SQL keyword to specify query predicates.
     */
    WHERE("WHERE"),

    /**
     * SQL keyword to join tables.
     */
    JOIN("JOIN"),

    /**
     * SQL keyword to inner join tables.
     */
    INNER_JOIN("INNER JOIN"),

    /**
     * SQL keyword to outer join tables.
     */
    OUTER_JOIN("OUTER JOIN"),

    /**
     * SQL keyword to left join tables.
     */
    LEFT_JOIN("LEFT JOIN"),

    /**
     * SQL keyword to specify operation target.
     */
    ON("ON"),

    /**
     * SQL keyword to specify column or table alias.
     */
    AS("AS"),

    /**
     * SQL keyword to specify an offset of the result set (used for paginating results).
     */
    OFFSET("OFFSET"),

    /**
     * SQL keyword to specify a size limit of the result set (used for paginating results).
     */
    LIMIT("LIMIT");

    private static final Map<QueryOperator.Type, SqlKeyword> QUERY_OPERATOR_TO_SQL = new HashMap<>();
    private static final Map<QueryPredicateGroup.Type, SqlKeyword> QUERY_PREDICATE_GROUP_TO_SQL = new HashMap<>();

    private final String stringValue;

    static {
        QUERY_OPERATOR_TO_SQL.put(QueryOperator.Type.EQUAL, SqlKeyword.EQUAL);
        QUERY_OPERATOR_TO_SQL.put(QueryOperator.Type.NOT_EQUAL, SqlKeyword.NOT_EQUAL);
        QUERY_OPERATOR_TO_SQL.put(QueryOperator.Type.GREATER_THAN, SqlKeyword.GREATER_THAN);
        QUERY_OPERATOR_TO_SQL.put(QueryOperator.Type.GREATER_OR_EQUAL, SqlKeyword.GREATER_OR_EQUAL);
        QUERY_OPERATOR_TO_SQL.put(QueryOperator.Type.LESS_THAN, SqlKeyword.LESS_THAN);
        QUERY_OPERATOR_TO_SQL.put(QueryOperator.Type.LESS_OR_EQUAL, SqlKeyword.LESS_OR_EQUAL);

        QUERY_PREDICATE_GROUP_TO_SQL.put(QueryPredicateGroup.Type.AND, SqlKeyword.AND);
        QUERY_PREDICATE_GROUP_TO_SQL.put(QueryPredicateGroup.Type.OR, SqlKeyword.OR);
        QUERY_PREDICATE_GROUP_TO_SQL.put(QueryPredicateGroup.Type.NOT, SqlKeyword.NOT);
    }

    SqlKeyword(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Retrieve the SQL specific keyword for the query operator type.
     * @param queryOperatorType the query operator type
     * @return the SQL specific keyword
     */
    public static SqlKeyword fromQueryOperator(@NonNull QueryOperator.Type queryOperatorType) {
        final SqlKeyword sqlKeyword = QUERY_OPERATOR_TO_SQL.get(Objects.requireNonNull(queryOperatorType));
        if (null == sqlKeyword) {
            throw new IllegalArgumentException(
                    "No SQL keyword mapping defined for query operator type = " + queryOperatorType.toString()
            );
        }
        return sqlKeyword;
    }

    /**
     * Retrieve the SQL specific keyword for the query predicate group type.
     * @param groupType the query predicate group type
     * @return the SQL specific keyword
     */
    public static SqlKeyword fromQueryPredicateGroup(@NonNull QueryPredicateGroup.Type groupType) {
        final SqlKeyword sqlKeyword = QUERY_PREDICATE_GROUP_TO_SQL.get(Objects.requireNonNull(groupType));
        if (null == sqlKeyword) {
            throw new IllegalArgumentException(
                    "No SQL keyword mapping defined for query predicate group type = " + groupType.toString()
            );
        }
        return sqlKeyword;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
