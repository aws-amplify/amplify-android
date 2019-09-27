package com.example.aws_amplify_api_okhttp;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Converter implements IConverter {
    private final Gson gson;

    public Converter(Gson gson) {
        this.gson = gson;
    }

    public Converter() {
        this.gson = new Gson();
    }

    @Override
    public <T> BodyConverter<T> bodyConverter() {
        return new GsonBodyConverter<T>(gson);
    }

    public static class GsonBodyConverter<T> implements Converter.BodyConverter<T> {

        private final Gson gson;

        public GsonBodyConverter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public T convert(String json, Class<T> classToCast, boolean toList) throws Exception {

            String dataJSon = null;
            try {
                final JSONObject toJson = new JSONObject(json);
                final JSONObject data, error;

                if (toJson.has("data")){
                    data = toJson.getJSONObject("data");

                    if (data.get("list") instanceof JSONArray){ //Should only cast if List<>

                        //https://stackoverflow.com/questions/9817315/how-to-check-whether-the-given-object-is-object-or-array-in-json-string
                        dataJSon = toJson.getJSONObject("data")
                                .getJSONArray("list").getJSONObject(0).toString();
                    } else {
                        dataJSon = data.toString();
                    }
                }

                if (toJson.has("error")){
                    error = toJson.getJSONObject("error");
                    //throw new ErrorBodyResponse(error); //Need a better way to return partial errors
                }
            } catch (JSONException e) {
                Log.e("JSON", e.getLocalizedMessage());
                e.printStackTrace();
            }

            if (toList) {
                Log.i("GSON56", "toList is true");
                final JSONObject toJson = new JSONObject(dataJSon);
                //final JSONObject list = toJson.getJSONObject("list");
                //return (T) gson.fromJson(list, classToCast);
                return null;
            } else {
                try {
                    return (T) gson.fromJson(dataJSon, classToCast);
                } catch (Exception e){
                    throw new ClassCastException(e.getLocalizedMessage());
                }
            }
        }

    }
}
