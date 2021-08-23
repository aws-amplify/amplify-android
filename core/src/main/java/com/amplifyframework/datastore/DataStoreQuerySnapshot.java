package com.amplifyframework.datastore;

import com.amplifyframework.core.model.Model;

import java.util.List;

public class DataStoreQuerySnapshot<T extends Model> {
    private final List<T> items;
    private final boolean isSynced;
    // Why do we have items and itemChanges and which do we return on
    private final List<DataStoreItemChange<T>> itemChanges;
    public DataStoreQuerySnapshot(List<T> items, boolean isSynced, List<DataStoreItemChange<T>> itemChanges){
        this.items = items;
        this.isSynced = isSynced;
        this.itemChanges = itemChanges;
    }

    public List<T> getItems(){
        return items;
    }

    public boolean getIsSynced(){
        return isSynced;
    }

    public List<DataStoreItemChange<T>> getItemChanges(){
        return itemChanges;
    }

}
