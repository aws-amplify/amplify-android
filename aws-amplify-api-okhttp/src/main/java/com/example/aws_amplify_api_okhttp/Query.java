package com.example.aws_amplify_api_okhttp;


import java.util.List;

public class Query<T> extends AbstractQuery<T, Query<T>> {
    public Query(OkhttpApiPlugin okhttpApiPlugin, String query) {
        this(okhttpApiPlugin, "query", query);
    }

    public Query(OkhttpApiPlugin okhttpApiPlugin, String name, String query) {
        super(okhttpApiPlugin, name, query);
    }

    public <R> Query<R> cast(Class<R> theClass){
        super.castClass(theClass);
        return (Query<R>)this;
    }

    //@Deprecated
    public <R> Query<List<R>> castList(Class<R> theClass) {
        super.castClassList(theClass);
        return (Query<List<R>>)this;
    }
}
