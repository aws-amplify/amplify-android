package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.Nullable;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;

public class ModelComparator<T extends Model> implements Comparator<T> {

    private final QuerySortBy sortBy;
    private final Class<T> itemClass;
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    public ModelComparator(QuerySortBy sortBy, Class<T> itemClass){
        this.sortBy = sortBy;
        this.itemClass = itemClass;
    }

    @Override
    public int compare(T model1, T model2) {
        if (sortBy != null && sortBy.getModelName() != null) {
            Method method =getMethod();
            if (method != null){
                Object value1 = getValue(method, model1);
                Object value2 = getValue(method, model2);
                if( value1 == null ){
                    return -1;
                }else if( value2 == null ){
                    return 1;
                }
                if (method.getReturnType()== String.class) {
                    String valueModel1 = (String) value1;
                    String valueModel2 = (String) value2;
                    return   valueModel1.compareTo(valueModel2);
                }
                if (method.getReturnType()== int.class) {
                    int valueModel1 = (int) value1;
                    int valueModel2 = (int) value2;
                    return Integer.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Integer.class) {
                    Integer valueModel1 = (Integer) value1;
                    Integer valueModel2 = (Integer) value2;
                    return Integer.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== boolean.class) {
                    boolean valueModel1 = (boolean) value1;
                    boolean valueModel2 = (boolean) value2;
                    return Boolean.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Boolean.class) {
                    Boolean valueModel1 = (Boolean) value1;
                    Boolean valueModel2 = (Boolean) value2;
                    return Boolean.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== long.class) {
                    long valueModel1 = (long) value1;
                    long valueModel2 = (long) value2;
                    return Long.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== double.class) {
                    double valueModel1 = (double) value1;
                    double valueModel2 = (double) value2;
                    return Double.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Double.class) {
                    Double valueModel1 = (Double) value1;
                    Double valueModel2 = (Double) value2;
                    return Double.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Date.class) {
                    Date valueModel1 = (Date) value1;
                    Date valueModel2 = (Date) value2;
                    return valueModel1.compareTo(valueModel2);
                }
                if (method.getReturnType()== float.class) {
                    float valueModel1 = (float) value1;
                    float valueModel2 = (float) value2;
                    return Float.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Float.class) {
                    Float valueModel1 = (Float) value1;
                    Float valueModel2 = (Float) value2;
                    return Float.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== char.class) {
                    char valueModel1 = (char) value1;
                    char valueModel2 = (char) value2;
                    return Character.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Character.class) {
                    Character valueModel1 = (Character) value1;
                    Character valueModel2 = (Character) value2;
                    return Character.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== byte.class) {
                    byte valueModel1 = (byte) value1;
                    byte valueModel2 = (byte) value2;
                    return Byte.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Byte.class) {
                    Byte valueModel1 = (Byte) value1;
                    Byte valueModel2 = (Byte) value2;
                    return Byte.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType()== Temporal.DateTime.class) {
                    Temporal.DateTime valueModel1 = (Temporal.DateTime) value1;
                    Temporal.DateTime valueModel2 = (Temporal.DateTime) value2;
                    return valueModel1.compareTo(valueModel2);
                }
                if (method.getReturnType()== Temporal.Date.class) {
                    Temporal.DateTime valueModel1 = (Temporal.DateTime) value1;
                    Temporal.DateTime valueModel2 = (Temporal.DateTime) value2;
                    return valueModel1.compareTo(valueModel2);
                }
            }
        }
        return -1;
    }

        private Method getMethod() {
            Method method = null;
            try {
                Field field = itemClass.getDeclaredField(sortBy.getField());
                if(field.getName().equals(sortBy.getField())){
                    method = getMethod(itemClass, field);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            return method;
        }

    @Nullable
    private Method getMethod(Class<?> className, Field field) {
        for (Method method : className.getMethods()) {
            if ((method.getName().startsWith("get")) && (method.getName().length() == (field.getName().length() + 3))) {
                if (method.getName().toLowerCase().endsWith(field.getName().toLowerCase())) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Object getValue(Method method, Model model)
    {
        try
        {
            return method.invoke(model);
        }
        catch (IllegalAccessException e)
        {
            LOG.debug("Could not invoke method during sorting because of access level" + method.getName());
        }
        catch (InvocationTargetException e)
        {
            LOG.debug("Could not invoke method during sorting " + method.getName());
        }
        return null;
    }

}


