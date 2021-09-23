package com.amplifyframework.datastore;

import com.amplifyframework.core.model.Model;

import java.util.List;

public class DataStoreQuerySnapshot<T extends Model> {
    private final List<T> items;
    private final boolean isSynced;

    public DataStoreQuerySnapshot( List<T> items, boolean isSynced ){
        this.items = items;
        this.isSynced = isSynced;
    }

    public List<T> getItems(){
        return items;
    }

    public boolean getIsSynced(){
        return isSynced;
    }


}
