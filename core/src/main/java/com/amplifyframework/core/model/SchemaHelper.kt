package com.amplifyframework.core.model

class SchemaHelper {

    fun getAssociatedKeys(schema: ModelSchema, modelField: ModelField): Map<String, List<String>>{
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