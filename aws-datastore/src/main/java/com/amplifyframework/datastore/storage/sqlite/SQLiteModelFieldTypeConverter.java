/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.AWSDate;
import com.amplifyframework.core.model.AWSDateTime;
import com.amplifyframework.core.model.AWSTime;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.model.ModelFieldTypeConverter;
import com.amplifyframework.datastore.model.ModelHelper;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <code>ModelField</code> value converter for SQLite. It converts from SQLite's <code>Cursor</code>
 * to <code>Model</code> properties and from <code>Model</code> properties to values that are
 * valid in a <code>SQLiteStatement</code>.
 */
public final class SQLiteModelFieldTypeConverter implements ModelFieldTypeConverter<Cursor, Model> {
    private static final Logger LOGGER = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final Class<? extends Model> modelType;
    private final ModelSchemaRegistry modelSchemaRegistry;
    private final Gson gson;
    private final Map<String, SQLiteColumn> columns;

    SQLiteModelFieldTypeConverter(
            @NonNull Class<? extends Model> modelType,
            @NonNull ModelSchemaRegistry modelSchemaRegistry,
            @NonNull Gson gson
    ) {
        this.modelSchemaRegistry = Objects.requireNonNull(modelSchemaRegistry);
        this.gson = Objects.requireNonNull(gson);
        this.modelType = modelType;

        // load and store the SQL columns for the modelType
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(
                modelSchemaRegistry.getModelSchemaForModelClass(modelType.getSimpleName()));
        this.columns = sqliteTable.getColumns();
    }

    /**
     * Helper that converts a given value to a {@code fieldType} to the correct SQLite type.
     *
     * @param value the field value
     * @param fieldType the field type as a enum
     * @param gson an optional {@code Gson} instance
     * @return the converted value
     * @see #convertValueFromSource(Cursor, ModelField)
     */
    public static Object convertRawValueToTarget(
            @Nullable final Object value,
            @NonNull final JavaFieldType fieldType,
            @Nullable Gson gson
    ) {
        if (value == null) {
            return null;
        }
        switch (fieldType) {
            case INTEGER:
            case LONG:
            case FLOAT:
            case STRING:
                // these types require no special treatment
                return value;
            case BOOLEAN:
                boolean booleanValue = (boolean) value;
                return booleanValue ? 1L : 0L;
            case MODEL:
                return ((Model) value).getId();
            case ENUM:
                return ((Enum) value).name();
            case CUSTOM_TYPE:
                final Gson jsonConverter = gson != null ? gson : new Gson();
                return jsonConverter.toJson(value);
            case DATE:
                return ((AWSDate) value).format();
            case DATE_TIME:
                return ((AWSDateTime) value).format();
            case TIME:
                return ((AWSTime) value).format();
            default:
                LOGGER.warn(String.format("Field of type %s is not supported. Fallback to null.", fieldType));
                return null;
        }
    }

    @Override
    public Object convertValueFromSource(
            @NonNull Cursor cursor,
            @NonNull ModelField field
    ) throws DataStoreException {
        final JavaFieldType javaFieldType = TypeConverter.getJavaFieldType(field);
        try {
            // Skip if there is no equivalent column for field in object
            final SQLiteColumn column = columns.get(field.getName());
            if (column == null) {
                LOGGER.warn(String.format("Column with name %s does not exist", field.getName()));
                return null;
            }

            final String columnName = column.getAliasedName();
            final int columnIndex = cursor.getColumnIndexOrThrow(columnName);
            // This check is necessary, because primitive values will return 0 even when null
            if (cursor.isNull(columnIndex)) {
                return null;
            }

            final String valueAsString = cursor.getString(columnIndex);
            LOGGER.verbose(String.format(
                    "Attempt to convert value \"%s\" from field %s of type %s from model %s",
                    valueAsString, field.getName(), field.getType(), modelType.getSimpleName()
            ));

            switch (javaFieldType) {
                case STRING:
                    return cursor.getString(columnIndex);
                case MODEL:
                    return convertModelAssociationToTarget(cursor, field);
                case ENUM:
                    return convertEnumValueToTarget(valueAsString, field);
                case CUSTOM_TYPE:
                    return convertCustomTypeToTarget(cursor, field, columnIndex);
                case INTEGER:
                    return cursor.getInt(columnIndex);
                case BOOLEAN:
                    return cursor.getInt(columnIndex) != 0;
                case FLOAT:
                    return cursor.getFloat(columnIndex);
                case LONG:
                    return cursor.getLong(columnIndex);
                case DATE:
                    return new AWSDate(valueAsString);
                case DATE_TIME:
                    return new AWSDateTime(valueAsString);
                case TIME:
                    return new AWSTime(valueAsString);
                default:
                    LOGGER.warn(String.format("Field of type %s is not supported. Fallback to null.", javaFieldType));
                    return null;
            }
        } catch (Exception exception) {
            throw new DataStoreException(
                    String.format("Error converting field \"%s\" from model \"%s\"",
                            field.getName(), modelType.getName()),
                    exception,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private Object convertModelAssociationToTarget(
            @NonNull Cursor cursor,
            @NonNull ModelField field
    ) throws DataStoreException {
        // Eager load model if the necessary columns are present inside the cursor.
        // At the time of implementation, cursor should have been joined with these
        // columns IF AND ONLY IF the model is a foreign key to the inner model.
        // value has Class<?>, but we want Class<? extends Model>
        @SuppressWarnings("unchecked")
        Class<? extends Model> nestedModelType = (Class<? extends Model>) field.getType();
        String className = nestedModelType.getSimpleName();
        ModelSchema innerModelSchema = modelSchemaRegistry.getModelSchemaForModelClass(className);

        SQLiteModelFieldTypeConverter nestedModelConverter =
                new SQLiteModelFieldTypeConverter(nestedModelType, modelSchemaRegistry, gson);

        Map<String, Object> mapForInnerModel = new HashMap<>();
        for (Map.Entry<String, ModelField> entry : innerModelSchema.getFields().entrySet()) {
            mapForInnerModel.put(entry.getKey(), nestedModelConverter.convertValueFromSource(cursor, entry.getValue()));
        }
        final String modelInJsonFormat = gson.toJson(mapForInnerModel);
        try {
            return gson.getAdapter(nestedModelType).fromJson(modelInJsonFormat);
        } catch (IOException exception) {
            LOGGER.warn(
                    String.format("Error converting from JSON value to %s", nestedModelType.getSimpleName()),
                    exception
            );
            return null;
        }
    }

    private Object convertCustomTypeToTarget(Cursor cursor, ModelField field, int columnIndex) throws IOException {
        final String stringValue = cursor.getString(columnIndex);
        return gson.getAdapter(field.getType()).fromJson(stringValue);
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> E convertEnumValueToTarget(
            @NonNull final String value,
            @NonNull ModelField field
    ) {
        Class<E> enumClazz = (Class<E>) field.getType().asSubclass(Enum.class);
        return Enum.valueOf(enumClazz, value);
    }

    @Override
    public Object convertValueFromTarget(Model model, ModelField field) throws DataStoreException {
        final Object fieldValue = ModelHelper.getValue(model, field);
        if (fieldValue == null) {
            return null;
        }
        final JavaFieldType javaFieldType = TypeConverter.getJavaFieldType(field);
        return convertRawValueToTarget(fieldValue, javaFieldType, gson);
    }

}
