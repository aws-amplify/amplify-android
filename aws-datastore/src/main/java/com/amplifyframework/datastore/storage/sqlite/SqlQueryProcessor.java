package com.amplifyframework.datastore.storage.sqlite;

import android.database.Cursor;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.GsonFactory;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SqlQueryProcessor {

    private final ModelSchemaRegistry modelSchemaRegistry;
    private final SQLCommandFactory sqlCommandFactory;
    private final SQLCommandProcessor sqlCommandProcessor;
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final Gson gson;

    SqlQueryProcessor(SQLCommandProcessor sqlCommandProcessor,
                      SQLCommandFactory sqlCommandFactory,
                      ModelSchemaRegistry modelSchemaRegistry){
        this.sqlCommandProcessor = sqlCommandProcessor;
        this.sqlCommandFactory = sqlCommandFactory;
        this.modelSchemaRegistry = modelSchemaRegistry;
        this.gson = GsonFactory.instance();
    }

    <T extends Model> List<T> queryOfflineData(@NonNull Class<T> itemClass,
                                               @NonNull QueryOptions options,
                                               @NonNull Consumer<DataStoreException> onError) {
        final ModelSchema modelSchema = modelSchemaRegistry.getModelSchemaForModelClass(itemClass.getSimpleName());
        final List<T> models = new ArrayList<>();

        try (Cursor cursor = sqlCommandProcessor.rawQuery(sqlCommandFactory.queryFor(modelSchema, options))) {
            LOG.debug("Querying item for: " + itemClass.getSimpleName());
            final SQLiteModelFieldTypeConverter converter =
                    new SQLiteModelFieldTypeConverter(modelSchema, modelSchemaRegistry, gson);

            if (cursor == null) {
                onError.accept(new DataStoreException(
                        "Error in getting a cursor to the table for class: " + itemClass.getSimpleName(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else if (cursor.moveToFirst()) {
                do {
                    Map<String, Object> map = converter.buildMapForModel(cursor);
                    String jsonString = gson.toJson(map);
                    models.add(gson.fromJson(jsonString, itemClass));
                } while (cursor.moveToNext());
            }
        } catch (Exception exception) {
            onError.accept(new DataStoreException(
                    "Error in querying the model.", exception,
                    "See attached exception for details."
            ));
        }
        return models;
    }
}
