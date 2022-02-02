package com.amplifyframework.core.model;

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class ModelPrimaryKey<T extends Model> implements Serializable {

    private final Serializable key;
    private final List<? extends Serializable> sortedKeys;
    private static final long serialVersionUID = 1L;

    public ModelPrimaryKey(Serializable key, Serializable... sortedKeys) {
        this.key = key;
        this.sortedKeys = Arrays.asList(sortedKeys);
    }

    public Serializable key() {
        return key;
    }

    public List<? extends Serializable> sortedKeys() {
        return sortedKeys;
    }

    public String getIdentifier() {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        for (Serializable sortKey:sortedKeys) {
            builder.append("#");
            builder.append(sortKey);
        }
      return builder.toString() ;
    }



    public static class Helper {
        public static QueryPredicate getQueryPredicate(Model model, String tableName, List<String> primaryKeyList) {
            QueryPredicate matchId = null;
            if(primaryKeyList.size() == 1 && !(model.resolveIdentifier() instanceof ModelPrimaryKey)){
                matchId = QueryField.field(tableName, primaryKeyList.get(0)).eq(model.resolveIdentifier());
            } else{
                ModelPrimaryKey<?> primaryKey = (ModelPrimaryKey<?>) model.resolveIdentifier();
                Iterator<?> sortKeyIterator = primaryKey.sortedKeys().listIterator();
                for (String key: primaryKeyList) {
                    if (matchId == null){
                        matchId = QueryField.field(tableName, key).eq(primaryKey.key().toString());
                    } else {
                        matchId.and(QueryField.field(tableName, key).eq(sortKeyIterator.next()));
                    }
                }
            }
            return matchId;
        }

        public static String getUniqueKey(Serializable uniqueId) {

            String uniqueStringId;
            if (uniqueId instanceof String) {
                uniqueStringId = (String) uniqueId;
            } else if (uniqueId instanceof ModelPrimaryKey) {
                uniqueStringId = ((ModelPrimaryKey<?>) uniqueId).getIdentifier();
            } else {
                throw (new IllegalStateException("Invalid Primary Key, It should either be of type String or composite Primary Key."));
            }
            return uniqueStringId;
        }
    }

}
