package com.amplifyframework.api.aws

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelIdentifier
import com.amplifyframework.core.model.ModelSchema
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.io.Serializable
import java.lang.reflect.Type

/**
 * Here we are Deserializing Model types and Injecting values into lazy list fields. Lazy list fields will be null
 * from the server unless the list was provided in the selection set.
 *
 * @param responseGson is a Gson object that does not have the model deserializer. Otherwise context.fromJson would
 * cause a recursion issue.
 */
internal class ModelDeserializer(
    private val responseGson: Gson,
    private val apiName: String?
) : JsonDeserializer<Model> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Model {
        val parent = responseGson.fromJson<Model>(json, typeOfT)
        val parentType = (typeOfT as Class<*>).simpleName
        val parentModelSchema = ModelSchema.fromModelClass(parent.javaClass)

        parentModelSchema.fields.filter { it.value.isModelList }.map { fieldMap ->
            val fieldToUpdate = parent.javaClass.getDeclaredField(fieldMap.key)
            fieldToUpdate.isAccessible = true
            if (fieldToUpdate.get(parent) == null) {
                val lazyField = fieldMap.value
                val lazyFieldModelSchema = AWSApiSchemaRegistry.getModelSchemaForModelClass(lazyField.targetType)

                val lazyFieldTargetNames = lazyFieldModelSchema
                    .associations
                    .entries
                    .first { it.value.associatedType == parentType }
                    .value
                    .targetNames

                val parentIdentifiers = parent.getSortedIdentifiers()

                val queryKeys = lazyFieldTargetNames.mapIndexed { idx, name ->
                    name to parentIdentifiers[idx]
                }.toMap()

                val modelList = ApiLazyModelList(lazyFieldModelSchema.modelClass, queryKeys, apiName)

                fieldToUpdate.isAccessible = true
                fieldToUpdate.set(parent, modelList)
            }
        }
        return parent
    }
}

private fun Model.getSortedIdentifiers(): List<Serializable> {
    return when (val identifier = resolveIdentifier()) {
        is ModelIdentifier<*> -> { listOf(identifier.key()) + identifier.sortedKeys() }
        else -> listOf(identifier.toString())
    }
}
