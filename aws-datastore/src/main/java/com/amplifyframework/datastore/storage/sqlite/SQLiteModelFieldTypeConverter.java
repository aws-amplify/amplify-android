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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.ModelFieldTypeConverter;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <code>ModelField</code> value converter for SQLite. It converts from SQLite's <code>Cursor</code>
 * to <code>Model</code> properties and from <code>Model</code> properties to values that are
 * valid in a <code>SQLiteStatement</code>.
 */
final class SQLiteModelFieldTypeConverter implements ModelFieldTypeConverter<Cursor, Model> {

    private static final Logger LOGGER = Amplify.Logging.forNamespace("amplify:aws-datastore:sqliteConverter");

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
                LOGGER.warn("Column with name " + field.getName() + " does not exist");
                return null;
            }

            final String columnName = column.getAliasedName();
            final int columnIndex = cursor.getColumnIndexOrThrow(columnName);
            // This check is necessary, because primitive values will return 0 even when null
            if (cursor.isNull(columnIndex)) {
                return null;
            }

            switch (javaFieldType) {
                case STRING:
                    return cursor.getString(columnIndex);
                case MODEL:
                    return convertModelAssociationToTarget(cursor, field);
                case ENUM:
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
                    return convertDateToTarget(cursor, field, columnIndex);
                case TIME:
                    final long timeInLongFormat = cursor.getLong(columnIndex);
                    return new Time(timeInLongFormat);
                default:
                    LOGGER.warn("Field of type " + javaFieldType + " is not supported. Fallback to null.");
                    return null;
            }
        } catch (Exception exception) {
            throw new DataStoreException(
                    "Error converting field " + field.getName() + " from " + modelType.getName(),
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
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
            LOGGER.warn("", exception);
            return null;
        }
    }

    private Object convertCustomTypeToTarget(Cursor cursor, ModelField field, int columnIndex) throws IOException {
        final String stringValue = cursor.getString(columnIndex);
        return gson.getAdapter(field.getType()).fromJson(stringValue);
    }

    private Object convertDateToTarget(Cursor cursor, ModelField field, int columnIndex) throws ParseException {
        // TODO wire up the new Date/Time handling here
        final String dateInStringFormat = cursor.getString(columnIndex);
        if (dateInStringFormat != null) {
            return SimpleDateFormat
                    .getDateInstance()
                    .parse(dateInStringFormat);
        }
        return null;
    }

    @Override
    public Object convertValueFromTarget(Model model, ModelField field) throws DataStoreException {
        final String fieldName = field.getName();

        final Object fieldValue = getModelValue(model, fieldName);
        if (fieldValue == null) {
            return null;
        }
        final JavaFieldType javaFieldType = TypeConverter.getJavaFieldType(field);

        switch (javaFieldType) {
            case INTEGER:
            case LONG:
            case FLOAT:
            case STRING:
                // these types require no special treatment
                return fieldValue;
            case BOOLEAN:
                boolean booleanValue = (boolean) fieldValue;
                return booleanValue ? 1L : 0L;
            case MODEL:
                return ((Model) fieldValue).getId();
            case ENUM:
            case CUSTOM_TYPE:
                return gson.toJson(fieldValue);
            case DATE:
                // TODO integrate with new Date/Time handling
                final Date dateValue = (Date) fieldValue;
                return SimpleDateFormat
                        .getDateInstance()
                        .format(dateValue);
            case TIME:
                return ((Time) fieldValue).getTime();
            default:
                LOGGER.warn("Field of type " + javaFieldType + " is not supported. Fallback to null.");
                return null;
        }
    }

    private Object getModelValue(Model model, String fieldName) throws DataStoreException {
        Class<? extends Model> modelClass = model.getClass();
        final String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            final Method fieldGetter = modelClass.getMethod(getterName);
            return fieldGetter.invoke(model);
        } catch (Exception exception) {
            LOGGER.debug("Could not find " + getterName + "() on " + modelClass.getName()
                    + ". Fallback to direct field access.");
            // fallback to direct field access
            try {
                final Field fieldReference = modelClass.getDeclaredField(fieldName);
                fieldReference.setAccessible(true);
                return fieldReference.get(model);
            } catch (Exception fallbackException) {
                throw new DataStoreException(
                        "Error when reading the property " + fieldName + " from class " + modelClass.getName(),
                        fallbackException,
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                );
            }
        }
    }

}
