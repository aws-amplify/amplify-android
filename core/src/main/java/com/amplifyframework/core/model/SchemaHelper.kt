/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model

class SchemaHelper {

    fun getAssociatedKeys(schema: ModelSchema, modelField: ModelField): Map<String, List<String>> {
        val associatedFieldsMap = HashMap<String, List<String>>()
        val associatedFields = ArrayList<String>()
        val association = schema.getAssociations().get(modelField.name)
        association?.run {
            getForeignKeyFields(schema, associatedFields)
            if (this.isOwner) {
                associatedFieldsMap.put(schema.name, associatedFields)
            } else {
                associatedFieldsMap.put(associatedName, associatedFields)
            }
        }
        return associatedFieldsMap
    }

    private fun ModelAssociation.getForeignKeyFields(
        schema: ModelSchema,
        associatedFields: ArrayList<String>) = if (schema.version >= 1 && !this.targetNames.isNullOrEmpty()) {
        // When target names length is more than 0 there are two scenarios, one is when
        // there is custom primary key and other is when we have composite primary key.
        associatedFields.addAll(targetNames.toList())
    } else {
        associatedFields.add(targetName)
    }
}
