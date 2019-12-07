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

package com.amplifyframework.datastore.storage.sqlite.adapter;

import com.amplifyframework.core.model.query.predicate.BeginsWithQueryOperator;
import com.amplifyframework.core.model.query.predicate.BetweenQueryOperator;
import com.amplifyframework.core.model.query.predicate.ContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.SqlKeyword;
import com.amplifyframework.util.Immutable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * SQL Predicate adapter that adapts {@link QueryPredicate} to be
 * compatible with SQLite use-cases. Adapted SQLPredicate object
 * can be used to obtain query string in SQLite WHERE statement
 * format alongside its selection arguments.
 *
 * For example:
 *<pre>
 *     {@code
 *     QueryPredicate nameCheck = QueryField.field("name").eq("Jane");
 *     SQLPredicate adapted = new SQLPredicate(nameCheck);
 *     System.out.println(adapted.toString()); // Prints "name = ?"
 *     System.out.println(adapted.getSelectionArgs()); // Prints "[Jane]"
 *     }
 *</pre>
 *
 */
public final class SQLPredicate {
    private final List<String> selectionArgs;
    private final String queryString;

    /**
     * Constructs an adapted instance of SQLPredicate
     * from an instance of {@link QueryPredicate}.
     * @param predicate query predicate to adapt
     * @throws DataStoreException If unable to parse the predicate
     */
    public SQLPredicate(QueryPredicate predicate) throws DataStoreException {
        this.selectionArgs = new LinkedList<>();
        this.queryString = parsePredicate(predicate).toString();
    }

    /**
     * Returns the SQL WHERE statement that is equivalent to
     * the provided instance of {@link QueryPredicate}.
     *
     * This is a prepared statement, where "?" represent values
     * to be replaced with variables, which can be obtained
     * by {@link SQLPredicate#getSelectionArgs()} method. The
     * method will return a list of strings to replace "?"s with
     * in the same order that they appear in the query string.
     *
     * @return the SQL WHERE statement in string form.
     */
    @Override
    public String toString() {
        return queryString;
    }

    /**
     * Returns the selection arguments for the converted query string.
     * @return the selection arguments for the converted query string.
     */
    public List<String> getSelectionArgs() {
        return Immutable.of(selectionArgs);
    }

    // Utility method to recursively parse a given predicate.
    private StringBuilder parsePredicate(QueryPredicate queryPredicate) throws DataStoreException {
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation qpo = (QueryPredicateOperation) queryPredicate;
            return parsePredicateOperation(qpo);
        } else if (queryPredicate instanceof QueryPredicateGroup) {
            QueryPredicateGroup qpg = (QueryPredicateGroup) queryPredicate;
            return parsePredicateGroup(qpg);
        } else {
            throw new DataStoreException(
                    "Tried to parse an unsupported QueryPredicate",
                    "Try changing to one of the supported values: " +
                            "QueryPredicateOperation, QueryPredicateGroup."
            );
        }
    }

    // Utility method to recursively parse a given predicate operation.
    private StringBuilder parsePredicateOperation(QueryPredicateOperation operation) throws DataStoreException {
        final StringBuilder builder = new StringBuilder();
        final String field = operation.field();
        final QueryOperator op = operation.operator();
        switch (op.type()) {
            case BETWEEN:
                BetweenQueryOperator betweenOp = (BetweenQueryOperator) op;
                Object start = betweenOp.start();
                Object end = betweenOp.end();
                QueryPredicateOperation gt = new QueryPredicateOperation(field,
                        new GreaterThanQueryOperator(start));
                QueryPredicateOperation lt = new QueryPredicateOperation(field,
                        new LessThanQueryOperator(end));
                return parsePredicate(gt.and(lt));
            case CONTAINS:
                ContainsQueryOperator containsOp = (ContainsQueryOperator) op;
                selectionArgs.add(containsOp.value().toString());
                return builder.append("?")
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.IN)
                        .append(SqlKeyword.DELIMITER)
                        .append(field);
            case BEGINS_WITH:
                BeginsWithQueryOperator beginsWithOp = (BeginsWithQueryOperator) op;
                selectionArgs.add(beginsWithOp.value() + "%");
                return builder.append(field)
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.LIKE)
                        .append(SqlKeyword.DELIMITER)
                        .append("?");
            case EQUAL:
            case NOT_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_OR_EQUAL:
            case GREATER_OR_EQUAL:
                selectionArgs.add(getOperatorValue(op).toString());
                return builder.append(field)
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.fromQueryOperator(op.type()))
                        .append(SqlKeyword.DELIMITER)
                        .append("?");
            default:
                throw new DataStoreException(
                        "Tried to parse an unsupported QueryPredicateOperation",
                        "Try changing to one of the supported values from " +
                                "QueryPredicateOperation.Type enum."
                );
        }
    }

    // Utility method to recursively parse a given predicate group.
    private StringBuilder parsePredicateGroup(QueryPredicateGroup group) throws DataStoreException {
        final StringBuilder builder = new StringBuilder();
        switch (group.type()) {
            case NOT:
                return builder.append(SqlKeyword.fromQueryPredicateGroup(group.type()))
                        .append(SqlKeyword.DELIMITER)
                        .append(parsePredicate(group.predicates().get(0)));
            case OR:
            case AND:
                builder.append("(");
                Iterator<QueryPredicate> predicateIterator = group.predicates().iterator();
                while (predicateIterator.hasNext()) {
                    builder.append(parsePredicate(predicateIterator.next()));
                    if (predicateIterator.hasNext()) {
                        builder.append(SqlKeyword.DELIMITER)
                                .append(SqlKeyword.fromQueryPredicateGroup(group.type()))
                                .append(SqlKeyword.DELIMITER);
                    }
                }
                return builder.append(")");
            default:
                throw new DataStoreException(
                        "Tried to parse an unsupported QueryPredicateGroup",
                        "Try changing to one of the supported values from " +
                                "QueryPredicateGroup.Type enum."
                );
        }
    }

    // Utility method to extract the parameter value from a given operator.
    private Object getOperatorValue(QueryOperator qOp) throws DataStoreException {
        switch (qOp.type()) {
            case NOT_EQUAL:
                return ((NotEqualQueryOperator) qOp).value();
            case EQUAL:
                return ((EqualQueryOperator) qOp).value();
            case LESS_OR_EQUAL:
                return ((LessOrEqualQueryOperator) qOp).value();
            case LESS_THAN:
                return ((LessThanQueryOperator) qOp).value();
            case GREATER_OR_EQUAL:
                return ((GreaterOrEqualQueryOperator) qOp).value();
            case GREATER_THAN:
                return ((GreaterThanQueryOperator) qOp).value();
            default:
                throw new DataStoreException(
                        "Tried to parse an unsupported QueryOperator type",
                        "Check if a new QueryOperator.Type enum has been created which is not supported" +
                                "in the AppSyncGraphQLRequestFactory."
                );
        }
    }
}
