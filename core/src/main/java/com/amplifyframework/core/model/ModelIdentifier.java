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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

/**
 * This class is a representation of the custom primary key.
 * @param <T> Model.
 */
public abstract class ModelIdentifier<T extends Model> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Serializable key;
    private final List<? extends Serializable> sortedKeys;

    /**
     * Constructor for Model Primary key class. Takes in partition key and an array of sort keys as parameters.
     * @param key Partition key.
     * @param sortedKeys Array of sort keys.
     */
    public ModelIdentifier(Serializable key, Serializable... sortedKeys) {
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
         * Returns string representation of the unique key.
         * @param uniqueId If its a single key pass in the key. In the case of composite key pass the Model primary key.
         * @return String representation of the unique key.
         */
        public static String getUniqueKey(Serializable uniqueId) {
            String uniqueStringId;
            try {
                if (uniqueId instanceof ModelIdentifier) {
                    uniqueStringId = ((ModelIdentifier<?>) uniqueId).getIdentifier();
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
            String uniqueIdentifier = null;
            try {
                List<String> primaryKeyFieldList = modelSchema.getPrimaryIndexFields();
                if (primaryKeyFieldList.size() == 1) {
                    return Objects.requireNonNull(serializedData.get(primaryKeyFieldList.get(0))).toString();
                }
                final ListIterator<String> primaryKeyListIterator = primaryKeyFieldList.listIterator();
                final Serializable partitionKey = (Serializable) serializedData.get(primaryKeyListIterator.next());
                final List<Serializable> sortKeys = new ArrayList<>();
                while (primaryKeyListIterator.hasNext()) {
                    sortKeys.add((Serializable) serializedData.get(primaryKeyListIterator.next()));
                }
                if (partitionKey != null) {
                    uniqueIdentifier = getIdentifier(partitionKey, sortKeys);
                }
            } catch (Exception exception) {
                throw (new IllegalStateException("Invalid Primary Key," +
                        " It should either be single field or of type composite primary key" +
                        " Primary Key." + exception));
            }
            return uniqueIdentifier;
        }

        /**
         * Takes in a key value and escapes " with "" and encapsulates with ".
         * @param key string value of primary key field which needs to be encapsulated and escaped.
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
         * If no sort key and key is a string type, do not encapsulate
         * @param key Primary key.
         * @param sortedKeys List of sort keys.
         * @return Concatenated key.
         */
        public static String getIdentifier(Serializable key, List<? extends Serializable> sortedKeys) {
            /*
            For backwards compatibility to allow creating ModelIdentifiers for CPK that doesn't
            contain a sort key, we should not encapsulate String keys with quotes. This would
            also allow us to begin creating ModelIdentifiers for all Models in the future
            */
            if (key instanceof String && sortedKeys.size() == 0) {
                return (String) key;
            }
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
