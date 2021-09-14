package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.query.QuerySortOrder;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModelSorter<T extends Model> {

    public void sort(QueryOptions options, List<T> completeList, Class<T> itemClass){
        if(options !=null && options.getSortBy() != null && options.getSortBy().size() > 0) {
            Comparator<T> comparator = getComparator(options.getSortBy(), itemClass);
            Collections.sort(completeList, comparator);
        }
    }

    private Comparator<T> getComparator(List<QuerySortBy> sortByList, Class<T> itemClass){
        QuerySortBy sortBy = sortByList.get(0);

        Comparator<T> comparator = new ModelComparator<T>(sortByList.get(0), itemClass);
        QuerySortOrder sortOrder = sortBy.getSortOrder();
        if(sortOrder == QuerySortOrder.DESCENDING){
            comparator = comparator.reversed();
        }

        for (int i = 1; i<sortByList.size(); i++){
            QuerySortBy nextSortBy = sortByList.get(i);
            Comparator<T> nextComparator = comparator.thenComparing(new ModelComparator<T>(nextSortBy, itemClass));
            if(nextSortBy.getSortOrder() == QuerySortOrder.DESCENDING){
                nextComparator.reversed();
            }
        }
        return comparator;
    }
}
