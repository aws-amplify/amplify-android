/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.rest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Range;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Response from rest request.
 */
public final class RestResponse {
    private final Data data;
    private final Code code;

    /**
     * Constructs a response for the rest operation with empty data.
     * @param statusCode Status code of the response
     */
    public RestResponse(int statusCode) {
        this(statusCode, null);
    }

    /**
     * Constructs a response for the rest operation.
     * @param statusCode Status code of the response
     * @param data Data returned by the operation
     */
    public RestResponse(int statusCode, byte[] data) {
        this.data = new Data(data);
        this.code = new Code(statusCode);
    }

    /**
     * Get the data of the response.
     * @return Data from the request.
     */
    public Data getData() {
        return data;
    }

    /**
     * Get the http status code of the response.
     * @return Valid status code. If the returned code is invalid, it returns -1.
     */
    public Code getCode() {
        return code;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        RestResponse that = (RestResponse) thatObject;
        if (!ObjectsCompat.equals(this.getData(), that.getData())) {
            return false;
        }
        return ObjectsCompat.equals(this.getCode(), that.getCode());
    }

    @Override
    public int hashCode() {
        int result = getData() != null ? getData().hashCode() : 0;
        result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "RestResponse{" +
            "data=" + data +
            ", code=" + code +
            '}';
    }

    /**
     * Data representing the response of the HTTP request.
     */
    public static final class Data {

        /**
         * Raw data returned by the response.
         */
        private final byte[] rawBytes;

        /**
         * Constructs a data object with the raw rawBytes.
         * @param rawBytes Raw bytes of the response.
         */
        public Data(byte[] rawBytes) {
            this.rawBytes = rawBytes == null ? null : Arrays.copyOf(rawBytes, rawBytes.length);
        }

        /**
         * Get the raw data.
         * @return Returns the raw data as byte array.
         */
        public byte[] getRawBytes() {
            return rawBytes;
        }

        /**
         * Returns the data as a string.
         * @return String representation of the byte array.
         */
        public String asString() {
            return new String(rawBytes);
        }

        /**
         * Returns the JSON object of the rawBytes array.
         * @return JSON representation.
         * @throws JSONException Exception for JSON errors.
         */
        public JSONObject asJSONObject() throws JSONException {
            return new JSONObject(asString());
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }
            Data data = (Data) thatObject;
            return ObjectsCompat.equals(getRawBytes(), data.getRawBytes());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(getRawBytes());
        }

        @NonNull
        @Override
        public String toString() {
            return "Data{" +
                "rawBytes=" + Arrays.toString(rawBytes) +
                '}';
        }
    }

    /**
     * Status code of the response.
     */
    public static final class Code {
        private static final Range<Integer> ALL_VALID_CODES = new Range<>(100, 599);
        private static final Range<Integer> SERVICE_FAILURE_CODES = new Range<>(500, 599);
        private static final Range<Integer> CLIENT_ERROR_CODES = new Range<>(400, 499);
        private static final Range<Integer> SUCCESS_CODES = new Range<>(200, 299);

        private final int statusCode;

        /**
         * Constructs the Code object.
         * @param statusCode status code it include.
         */
        Code(int statusCode) {
            this.statusCode = validateValue(statusCode);
        }

        /**
         * Check if the status code is valid and returns same if valid or -1 if invalid.
         * @param statusCode Status code to check.
         * @return Input if valid or -1 if invalid.
         */
        private int validateValue(int statusCode) {
            return ALL_VALID_CODES.contains(statusCode) ? statusCode : -1;
        }

        /**
         * Returns true if the status code is of range 5xx.
         * @return true if service failure occurred.
         */
        public boolean isServiceFailure() {
            return SERVICE_FAILURE_CODES.contains(statusCode);
        }

        /**
         * Returns true if the status code is of range 4xx.
         * @return true if client error occurred.
         */
        public boolean isClientError() {
            return CLIENT_ERROR_CODES.contains(statusCode);
        }

        /**
         * Returns true if the status code is of range 2xx.
         * @return true if response has success code.
         */
        public boolean isSuccessful() {
            return SUCCESS_CODES.contains(statusCode);
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }
            Code code = (Code) thatObject;
            return statusCode == code.statusCode;
        }

        @Override
        public int hashCode() {
            return statusCode;
        }

        @NonNull
        @Override
        public String toString() {
            return "Code{" +
                "statusCode=" + statusCode +
                '}';
        }
    }
}
