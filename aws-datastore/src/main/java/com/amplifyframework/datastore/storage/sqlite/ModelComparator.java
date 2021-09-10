package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QuerySortBy;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

public class ModelComparator<T extends Model> implements Comparator<T> {

    public ModelComparator(){

    }


    @Override
    public int compare(T o1, T o2) {
        return 0;
    }

    private void sortTheList(List<QuerySortBy> sortByList){
        if (sortByList == null || sortByList.size() == 0) return;
        Comparator<T> comparator = null;
        try {
            for (QuerySortBy sortBy: sortByList) {
                if (sortBy != null && sortBy.getModelName() != null) {
                    Class<?> className = Class.forName(sortBy.getModelName());
                    Method method
                            = className.getDeclaredMethod("get"+sortBy.getField());


                        //comparator = Comparator.comparing(className::g)
                        //.reversed();
                        //.thenComparing(BlogOwner::getWea);
                }
            }
//            if (comparator != null){
//                //Collections.sort(completeItemList, comparator);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
