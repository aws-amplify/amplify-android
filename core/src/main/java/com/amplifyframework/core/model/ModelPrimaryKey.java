/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model;

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class is a representation of the custom primary key.
 * @param <T> Model.
 */
public abstract class ModelPrimaryKey<T extends Model> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Serializable key;
    private final List<? extends Serializable> sortedKeys;

    /**
     * Constructor for Model Primary key class. Takes in partition key and an array of sort keys as parameters.
     * @param key Partition key.
     * @param sortedKeys Array of sort keys.
     */
    public ModelPrimaryKey(Serializable key, Serializable... sortedKeys) {
        this.key = key;
        this.sortedKeys = Arrays.asList(sortedKeys);
    }

    /**
     * Returns the Partition key of the model.
     * @return Partition key.
     */
    public Serializable key() {
        return key;
    }

    /**
     * Returns the Array of sort keys of the model.
     * @return Array of sort keys.
     */
    public List<? extends Serializable> sortedKeys() {
        return sortedKeys;
    }

    /**
     * Returns a concatenated string of unique identifier. Concatenates PartitionKey#Sortkey1#sortkey2...
     * @return string representation of unique identifier
     */
    public String getIdentifier() {
        return Helper.getIdentifier(key, sortedKeys);
    }

    /**
     * Helper class for functions related to primary key.
     */
    public static class Helper {
        /**
         * Character which is used to encapsulate a primary key field while concatenating primary key fields.
         */
        public static final String PRIMARY_KEY_ENCAPSULATE_CHAR = "\"";

        /**
         * Character which is used as delimiter while concatenating primary key fields.
         */
        public static final String PRIMARY_KEY_DELIMITER = "#";

        /**
         * Helper function which creates a query predicate which returns.
         * @param model Model to create the predicate rom.
         * @param tableName Table name for which predicate needs to be created.
         * @param primaryKeyList List of primary key field value list.
         * @return Query Predicate to query with a unique identifier.
         */
        public static QueryPredicate getQueryPredicate(Model model, String tableName, List<String> primaryKeyList) {
            QueryPredicate matchId = null;
            if (!(model.resolveIdentifier() instanceof ModelPrimaryKey)) {
                matchId = QueryField.field(tableName, primaryKeyList.get(0)).eq(model.resolveIdentifier());
            } else {
                ModelPrimaryKey<?> primaryKey = (ModelPrimaryKey<?>) model.resolveIdentifier();
                Iterator<?> sortKeyIterator = primaryKey.sortedKeys().listIterator();
                for (String key : primaryKeyList) {
                    if (matchId == null) {
                        matchId = QueryField.field(tableName, key).eq(primaryKey.key().toString());
                    } else {
                        matchId.and(QueryField.field(tableName, key).eq(sortKeyIterator.next()));
                    }
                }
            }
            return matchId;
        }

        /**
         * Returns string representation of the unique key.
         * @param uniqueId If its a single key pass in the key. In the case of composite key pass the Model primary key.
         * @return String representation of the unique key.
         */
        public static String getUniqueKey(Serializable uniqueId) {
            String uniqueStringId;
            try {
                if (uniqueId instanceof ModelPrimaryKey) {
                    uniqueStringId = ((ModelPrimaryKey<?>) uniqueId).getIdentifier();
                } else {
                    uniqueStringId = uniqueId.toString();
                }
            } catch (Exception exception) {
                throw (new IllegalStateException("Invalid Primary Key," +
                        " It should either be of type String or composite" +
                        " Primary Key." + exception));
            }
            return uniqueStringId;
        }

        /**
         * Returns string representation of the unique key from schema and Serialized Data.
         * @param modelSchema Schema of the model.
         * @param serializedData key value representation of the data.
         * @return String representation of the unique key.
         */
        public static String getUniqueKey(ModelSchema modelSchema, Map<String, Object> serializedData) {
            StringBuilder id = new StringBuilder();
            try {
                final ListIterator<String> primaryKeyListIterator = modelSchema.getPrimaryIndexFields().listIterator();
                if (primaryKeyListIterator.hasNext()) {
                    id.append(serializedData.get(primaryKeyListIterator.next()));
                    if (primaryKeyListIterator.hasNext()) {
                        id.append("#");
                    }
                }
            } catch (Exception exception) {
                throw (new IllegalStateException("Invalid Primary Key," +
                        " It should either be single field or of type composite primary key" +
                        " Primary Key." + exception));
            }
            return id.toString();
        }

        /**
         * Takes in a key value and escapes " with "" and encapsulates with ".
         * @param key string value of primary key field which need needs to be encapsulated and escaped.
         * @return encapsulated and escaped string.
         */
        public static String escapeAndEncapsulateString(String key) {
            return PRIMARY_KEY_ENCAPSULATE_CHAR +
                    key.replaceAll(PRIMARY_KEY_ENCAPSULATE_CHAR, PRIMARY_KEY_ENCAPSULATE_CHAR
                            + PRIMARY_KEY_ENCAPSULATE_CHAR) + PRIMARY_KEY_ENCAPSULATE_CHAR;
        }

        /**
         * Concatenates primary key and sort keys after encapsulating them with '"', escaping the '"' with '""' and
         * delimited by '#'.
         * @param key Primary key.
         * @param sortedKeys List of sort keys.
         * @return Concatenated key.
         */
        public static String getIdentifier(Serializable key, List<? extends Serializable> sortedKeys) {
            StringBuilder builder = new StringBuilder();
            builder.append(Helper.escapeAndEncapsulateString(key.toString()));
            for (Serializable sortKey : sortedKeys) {
                builder.append(PRIMARY_KEY_DELIMITER);
                builder.append(Helper.escapeAndEncapsulateString(sortKey.toString()));
            }
            return builder.toString();
        }
    }
}
