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
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.model.ModelFieldTypeConverter;
import com.amplifyframework.datastore.model.ModelHelper;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.UserAgent;

import com.google.gson.Gson;

import java.io.IOException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <code>ModelField</code> value converter for SQLite. It converts from SQLite's <code>Cursor</code>
 * to <code>Model</code> properties and from <code>Model</code> properties to values that are
 * valid in a <code>SQLiteStatement</code>.
 */
public final class SQLiteModelFieldTypeConverter implements ModelFieldTypeConverter<Cursor, Model> {
    private static final Logger LOGGER = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final ModelSchema parentSchema;
    private final SchemaRegistry schemaRegistry;
    private final Gson gson;
    private final Map<String, SQLiteColumn> columns;

    // Map from inner model name to number of occurrences in the cursor.
    private final Map<String, Integer> cursorInnerModelCounts;
    private final boolean isInnerModel;

    SQLiteModelFieldTypeConverter(
            @NonNull ModelSchema parentSchema,
            @NonNull SchemaRegistry schemaRegistry,
            @NonNull Gson gson
    ) {
        this(parentSchema, schemaRegistry, gson, new HashMap<>());
    }

    private SQLiteModelFieldTypeConverter(
            @NonNull ModelSchema parentSchema,
            @NonNull SchemaRegistry schemaRegistry,
            @NonNull Gson gson,
            @NonNull Map<String, Integer> innerModelCounts
    ) {
        this.parentSchema = Objects.requireNonNull(parentSchema);
        this.schemaRegistry = Objects.requireNonNull(schemaRegistry);
        this.gson = Objects.requireNonNull(gson);
        this.columns = SQLiteTable.fromSchema(parentSchema).getColumns();
        this.cursorInnerModelCounts = innerModelCounts;
        this.isInnerModel = !this.cursorInnerModelCounts.isEmpty();
        if (!this.isInnerModel) {
            this.cursorInnerModelCounts.put(parentSchema.getName(), 1);
        }
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
            @NonNull Gson gson
    ) {
        if (value == null) {
            return null;
        }
        Objects.requireNonNull(fieldType);
        Objects.requireNonNull(gson);

        switch (fieldType) {
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case STRING:
                // these types require no special treatment
                return value;
            case BOOLEAN:
                boolean booleanValue = (boolean) value;
                return booleanValue ? 1L : 0L;
            case MODEL:
                return value instanceof Map ? ((Map<?, ?>) value).get("id") : ((Model) value).getPrimaryKeyString();
            case ENUM:
                return value instanceof String ? value : ((Enum<?>) value).name();
            case CUSTOM_TYPE:
                return gson.toJson(value);
            case DATE:
                return value instanceof String ? value : ((Temporal.Date) value).format();
            case DATE_TIME:
                if (UserAgent.isFlutter() && value instanceof String) {
                    return value;
                }
                OffsetDateTime offsetDateTime;
                if (value instanceof String) {
                    offsetDateTime = OffsetDateTime.parse((String) value);
                } else {
                    Temporal.DateTime newValue = (Temporal.DateTime) value;
                    offsetDateTime = OffsetDateTime.parse(newValue.format());
                }
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                        .ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");
                return offsetDateTime.toInstant().atOffset(ZoneOffset.UTC).format(dateTimeFormatter);
            case TIME:
                if (UserAgent.isFlutter() && value instanceof String) {
                    return value;
                }
                String timeValue;
                if (value instanceof String) {
                    timeValue = (String) value;
                } else {
                    timeValue = ((Temporal.Time) value).format();
                }
                LocalTime localTime;
                ZoneOffset zoneOffset;
                try {
                    OffsetTime offsetTime = OffsetTime.parse(timeValue, DateTimeFormatter.ISO_OFFSET_TIME);
                    localTime = LocalTime.from(offsetTime);
                    zoneOffset = ZoneOffset.from(offsetTime);
                } catch (Exception exception) {
                    localTime = LocalTime.parse(timeValue, DateTimeFormatter.ISO_LOCAL_TIME);
                    zoneOffset = ZoneOffset.UTC;
                }
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSSSS'Z'");
                return OffsetTime.of(localTime, zoneOffset).withOffsetSameInstant(ZoneOffset.UTC)
                        .format(timeFormatter);
            case TIMESTAMP:
                if (value instanceof Integer) {
                    return ((Integer) value).longValue();
                } else if (value instanceof Long) {
                    return value;
                }
                return ((Temporal.Timestamp) value).getSecondsSinceEpoch();
            default:
                LOGGER.warn(String.format("Field of type %s is not supported. Fallback to null.", fieldType));
                return null;
        }
    }

    Map<String, Object> buildMapForModel(@NonNull Cursor cursor) throws DataStoreException {
        final Map<String, Object> mapForModel = new HashMap<>();
        for (Map.Entry<String, ModelField> entry : parentSchema.getFields().entrySet()) {
            mapForModel.put(entry.getKey(), convertValueFromSource(cursor, entry.getValue()));
        }
        if (!this.isInnerModel) {
            cursorInnerModelCounts.clear();
            cursorInnerModelCounts.put(parentSchema.getName(), 1);
        }
        return mapForModel;
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
                LOGGER.verbose(String.format("Column with name %s does not exist", field.getName()));
                return null;
            }

            String columnName = column.getAliasedName();
            if (javaFieldType == JavaFieldType.MODEL) {
                int newInnerModelCount = 1;
                String fieldTargetType = field.getTargetType();
                if (cursorInnerModelCounts.containsKey(fieldTargetType)) {
                    Integer currentInnerModelCount = cursorInnerModelCounts.get(fieldTargetType);
                    newInnerModelCount += currentInnerModelCount == null ? 0 : currentInnerModelCount;
                }
                cursorInnerModelCounts.put(fieldTargetType, newInnerModelCount);
            }
            if (isInnerModel && cursorInnerModelCounts.containsKey(parentSchema.getName())) {
                Integer modelCount = cursorInnerModelCounts.get(parentSchema.getName());
                if (!Objects.equals(modelCount, 1)) {
                    // More than 1 of the model the field belongs to is present in the cursor
                    columnName += modelCount;
                }
            }
            
            final int columnIndex = cursor.getColumnIndexOrThrow(columnName);
            // This check is necessary, because primitive values will return 0 even when null
            if (cursor.isNull(columnIndex)) {
                return null;
            }

            final String valueAsString = cursor.getString(columnIndex);
            LOGGER.verbose(String.format(
                    "Attempt to convert value \"%s\" from field %s of type %s in model %s",
                    valueAsString, field.getName(), field.getTargetType(), parentSchema.getName()
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
                case DOUBLE:
                    return cursor.getDouble(columnIndex);
                case LONG:
                    return cursor.getLong(columnIndex);
                case DATE:
                    return new Temporal.Date(valueAsString);
                case DATE_TIME:
                    return new Temporal.DateTime(valueAsString);
                case TIME:
                    return new Temporal.Time(valueAsString);
                case TIMESTAMP:
                    return new Temporal.Timestamp(cursor.getLong(columnIndex), TimeUnit.SECONDS);
                default:
                    LOGGER.warn(String.format("Field of type %s is not supported. Fallback to null.", javaFieldType));
                    return null;
            }
        } catch (Exception exception) {
            throw new DataStoreException(
                    String.format("Error converting field \"%s\" from model \"%s\"",
                    field.getName(), parentSchema.getName()),
                    exception,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private Object convertModelAssociationToTarget(
            @NonNull Cursor cursor, @NonNull ModelField field) throws DataStoreException {
        // Eager load model if the necessary columns are present inside the cursor.
        // At the time of implementation, cursor should have been joined with these
        // columns IF AND ONLY IF the model is a foreign key to the inner model.
        ModelSchema innerModelSchema =
            schemaRegistry.getModelSchemaForModelClass(field.getTargetType());
        SQLiteModelFieldTypeConverter nestedModelConverter =
            new SQLiteModelFieldTypeConverter(innerModelSchema, schemaRegistry, gson, cursorInnerModelCounts);
        return nestedModelConverter.buildMapForModel(cursor);
    }

    private Object convertCustomTypeToTarget(Cursor cursor, ModelField field, int columnIndex) throws IOException {
        final String stringValue = cursor.getString(columnIndex);
        return gson.getAdapter(Objects.requireNonNull(field.getJavaClassForValue()))
            .fromJson(stringValue);
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> E convertEnumValueToTarget(
            @NonNull final String value, @NonNull ModelField field) {
        Class<E> enumClazz = (Class<E>)
            Objects.requireNonNull(field.getJavaClassForValue())
                .asSubclass(Enum.class);
        return Enum.valueOf(enumClazz, value);
    }

    @Override
    public Object convertValueFromTarget(Model model, ModelField field) throws DataStoreException {
        Object fieldValue;
        if (model.getClass() == SerializedModel.class) {
            fieldValue = ((SerializedModel) model).getValue(field);
        } else {
            fieldValue = ModelHelper.getValue(model, field);
        }
        if (fieldValue == null) {
            return null;
        }
        final JavaFieldType javaFieldType = TypeConverter.getJavaFieldType(field);
        return convertRawValueToTarget(fieldValue, javaFieldType, gson);
    }
}
