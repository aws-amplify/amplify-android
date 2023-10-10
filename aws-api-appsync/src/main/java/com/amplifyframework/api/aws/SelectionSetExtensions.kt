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

@file:JvmName("SelectionSetUtils")

package com.amplifyframework.api.aws

import com.amplifyframework.api.graphql.QueryType
import com.amplifyframework.core.model.PropertyContainerPath

/**
 * Find a child in the tree matching its `value`.
 *
 * @param name: the name to match the child node of type `SelectionSetField`
 * @return the matched `SelectionSet` or `nil` if there's no child with the specified name.
 */
internal fun SelectionSet.findChildByName(name: String) = nodes.find { it.value == name }

/**
 * Replaces or adds a new child to the selection set tree. When a child node exists
 * with a matching `value` property of the `SelectionSet` the node will be replaced
 * while retaining its position in the children list. Otherwise the call is
 * delegated to `nodes.add()`.
 *
 * @param selectionSet: the child node to be replaced.
 */
internal fun SelectionSet.replaceChild(selectionSet: SelectionSet) {
    this.nodes.removeIf { it.value == selectionSet.value }
    this.nodes.add(selectionSet)
}

/**
 * Transforms the entire property path (walking up the tree) into a `SelectionSet`.
 */
internal fun PropertyContainerPath.asSelectionSetWithoutRoot(): SelectionSet? {
    // create a lookup to hold info on whether or not the selection set is a collection or not
    val isCollectionLookup = mutableListOf<Boolean>()
    val selectionSets = nodesInPath(this, false).map {
        // always add to lookup list so that indexes match
        isCollectionLookup.add(it.getMetadata().isCollection)
        getSelectionSet(it)
    }

    if (selectionSets.isEmpty()) {
        return null
    }

    return selectionSets.reduceIndexed { i, acc, selectionSet ->
        if (isCollectionLookup[i]) {
            selectionSet.nodes.find { it.value == "items" }?.replaceChild(acc)
        } else {
            selectionSet.replaceChild(acc)
        }
        selectionSet
    }
}

private fun getSelectionSet(node: PropertyContainerPath): SelectionSet {
    val metadata = node.getMetadata()
    val name = if (metadata.isCollection) "items" else metadata.name

    var selectionSet = SelectionSet.builder()
        .operation(QueryType.GET)
        .value(name)
        .requestOptions(ApiGraphQLRequestOptions(0))
        .modelClass(node.getModelType())
        .build()

    if (metadata.isCollection) {
        selectionSet = SelectionSet(metadata.name, mutableSetOf(selectionSet))
    }

    return selectionSet
}

private fun shouldProcessNode(node: PropertyContainerPath, includeRoot: Boolean): Boolean {
    return includeRoot || node.getMetadata().parent != null
}

private fun nodesInPath(node: PropertyContainerPath, includeRoot: Boolean): List<PropertyContainerPath> {
    var currentNode: PropertyContainerPath? = node
    val path = mutableListOf<PropertyContainerPath>()

    while (currentNode != null && shouldProcessNode(currentNode, includeRoot)) {
        path.add(currentNode)
        currentNode = currentNode.getMetadata().parent as? PropertyContainerPath
    }
    return path
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
@JvmName("mergeChild")
internal fun SelectionSet.mergeChild(selectionSet: SelectionSet) {
    val name = selectionSet.value ?: ""
    val existingField = findChildByName(name)

    if (existingField != null) {
        val replaceFields = mutableListOf<SelectionSet>()
        selectionSet.nodes.forEach { child ->
            val childName = child.value
            if (child.nodes.isNotEmpty() && childName != null) {
                if (existingField.findChildByName(childName) != null) {
                    existingField.mergeChild(child)
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
