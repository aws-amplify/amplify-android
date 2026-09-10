/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelIdentifier
import com.amplifyframework.core.model.ModelList
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.ModelReference
import com.amplifyframework.core.model.annotations.BelongsTo
import com.amplifyframework.core.model.annotations.HasMany
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.core.model.annotations.ModelField
import java.io.Serializable

/**
 * Models for the relationships the code-generated test models do not cover: a parent identified by a
 * composite key, and a pair whose relationship only one side declares.
 *
 * Shaped the way generated models are — annotated fields left null, a `resolveIdentifier` that returns a
 * [ModelIdentifier] for a composite key — because the field-filling pass reads exactly that shape.
 */

/** A parent whose primary key is a partition key plus a sort key. */
@ModelConfig(pluralName = "Orders", hasLazySupport = true)
internal class Order : Model {
    @field:ModelField(targetType = "ID", isRequired = true)
    val customerId: String? = null

    @field:ModelField(targetType = "String", isRequired = true)
    val orderNumber: String? = null

    @field:ModelField(targetType = "OrderItem")
    @field:HasMany(associatedWith = "order", type = OrderItem::class)
    val items: ModelList<OrderItem>? = null

    override fun resolveIdentifier(): Serializable = OrderIdentifier(customerId!!, orderNumber!!)

    internal class OrderIdentifier(customerId: String, orderNumber: String) :
        ModelIdentifier<Order>(customerId, orderNumber)
}

/** The child of [Order], pointing back at it through one foreign key field per identifying value. */
@ModelConfig(pluralName = "OrderItems", hasLazySupport = true)
internal class OrderItem : Model {
    @field:ModelField(targetType = "ID", isRequired = true)
    val id: String? = null

    @field:ModelField(targetType = "Order", isRequired = true)
    @field:BelongsTo(targetNames = ["orderCustomerId", "orderOrderNumber"], type = Order::class)
    val order: ModelReference<Order>? = null

    override fun resolveIdentifier(): Serializable = id!!
}

/** A parent that claims a relationship [Entry] does not declare. */
@ModelConfig(pluralName = "Tags", hasLazySupport = true)
internal class Tag : Model {
    @field:ModelField(targetType = "ID", isRequired = true)
    val id: String? = null

    @field:ModelField(targetType = "Entry")
    @field:HasMany(associatedWith = "tag", type = Entry::class)
    val entries: ModelList<Entry>? = null

    override fun resolveIdentifier(): Serializable = id!!
}

/** The other side of [Tag]'s relationship, which it does not point back along. */
@ModelConfig(pluralName = "Entries", hasLazySupport = true)
internal class Entry : Model {
    @field:ModelField(targetType = "ID", isRequired = true)
    val id: String? = null

    override fun resolveIdentifier(): Serializable = id!!
}

internal class FixtureModelProvider(private val models: Set<Class<out Model>>) : ModelProvider {
    override fun models() = models
    override fun version() = "fixture"
}
