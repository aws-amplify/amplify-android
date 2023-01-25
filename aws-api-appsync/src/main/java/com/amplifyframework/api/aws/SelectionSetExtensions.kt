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

@file:JvmName("SelectionSetUtils")

package com.amplifyframework.api.aws

import com.amplifyframework.api.aws.SelectionSetDepth.Companion.onlyIncluded
import com.amplifyframework.api.graphql.QueryType
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.PropertyContainerPath

/**
 * Find a child in the tree matching its `value`.
 *
 * @param name: the name to match the child node of type `SelectionSetField`
 * @return the matched `SelectionSet` or `nil` if there's no child with the specified name.
 */
fun SelectionSet.findChildByName(name: String) = nodes.find { it.value == name }

/**
 * Replaces or adds a new child to the selection set tree. When a child node exists
 * with a matching `value` property of the `SelectionSet` the node will be replaced
 * while retaining its position in the children list. Otherwise the call is
 * delegated to `nodes.add()`.
 *
 * @param selectionSet: the child node to be replaced.
 */
fun SelectionSet.replaceChild(selectionSet: SelectionSet) {
    this.nodes.removeIf { it.value == selectionSet.value }
    this.nodes.add(selectionSet)
}

/**
 * Transforms the entire property path (walking up the tree) into a `SelectionSet`.
 */
fun PropertyContainerPath.asSelectionSet(includeRoot: Boolean = true): SelectionSet {
    val metadata = getMetadata()
    val name = if (metadata.isCollection) "items" else metadata.name
    var selectionSet = SelectionSet.builder()
        .operation(QueryType.GET)
        .value(name)
        .requestOptions(onlyIncluded())
        .modelClass(getModelType())
        .build()

    if (metadata.isCollection) {
        selectionSet = SelectionSet(metadata.name, setOf(selectionSet))
    }

    val parent = metadata.parent as? PropertyContainerPath
    if (parent != null && (parent.getMetadata().parent != null || includeRoot)) {
        val parentSelectionSet = parent.asSelectionSet(includeRoot)
        parentSelectionSet.replaceChild(selectionSet)
        selectionSet = parentSelectionSet
    }

    return selectionSet
}

/**
 * Merges a subtree into the this `SelectionSet`. The subtree position will be determined
 * by the value of the node's `name`. When an existing node is found the algorithm will
 * merge its children to ensure no values are lost or incorrectly overwritten.
 *
 * @param selectionSet the subtree to be merged into the current tree.
 *
 * @see findChildByName
 * @see replaceChild
 */
@JvmName("merge")
fun SelectionSet.mergeWith(selectionSet: SelectionSet) {
    val name = selectionSet.value ?: ""
    val existingField = findChildByName(name)

    if (existingField != null) {
        val replaceFields = mutableListOf<SelectionSet>()
        selectionSet.nodes.forEach { child ->
            val childName = child.value
            if (child.nodes.isNotEmpty() && childName != null) {
                if (existingField.findChildByName(childName) != null) {
                    existingField.mergeWith(child)
                } else {
                    replaceFields.add(child)
                }
            } else {
                replaceFields.add(child)
            }
        }
        replaceFields.forEach(existingField::replaceChild)
    } else {
        nodes.add(selectionSet)
    }
}