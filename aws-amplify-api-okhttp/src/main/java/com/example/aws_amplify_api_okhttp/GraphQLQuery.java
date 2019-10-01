package com.example.aws_amplify_api_okhttp;

import java.util.List;

public class GraphQLQuery<T> extends AbstractQuery<T, Query<T>> {
    public GraphQLQuery(OkhttpApiPlugin okhttpApiPlugin, String query) {
        this(okhttpApiPlugin, "query", query);
    }

    public GraphQLQuery(OkhttpApiPlugin okhttpApiPlugin, String name, String query) {
        super(okhttpApiPlugin, name, query);
    }

    public <R> GraphQLQuery<R> cast(Class<R> theClass){
        super.castClass(theClass);
        return (GraphQLQuery<R>)this;
    }

    //@Deprecated
    public <R> GraphQLQuery<List<R>> castList(Class<R> theClass) {
        super.castClassList(theClass);
        return (GraphQLQuery<List<R>>)this;
    }
}
