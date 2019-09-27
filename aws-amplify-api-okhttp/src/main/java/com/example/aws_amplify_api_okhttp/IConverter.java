package com.example.aws_amplify_api_okhttp;

public interface IConverter {
    interface BodyConverter<T> {
        T convert(String json, Class<T> classToCast, boolean toList) throws Exception;
    }

    <T> BodyConverter<T> bodyConverter();
}
