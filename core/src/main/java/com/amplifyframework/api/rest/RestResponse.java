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

    /**
     * Data representing the response of the HTTP request.
     */
    public static final class Data {

        /**
         * Data returned by the response.
         */
        private final byte[] bytes;

        /**
         * Constructs a data object with the raw bytes.
         * @param bytes Raw bytes of the response.
         */
        public Data(byte[] bytes) {
            this.bytes = bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
        }

        /**
         * Get the raw data.
         * @return Returns the raw data as byte array.
         */
        public byte[] getData() {
            return bytes;
        }

        /**
         * Returns the data as a string.
         * @return String representation of the byte array.
         */
        public String asString() {
            return new String(bytes);
        }

        /**
         * Returns the JSON object of the bytes array.
         * @return JSON representation.
         * @throws JSONException Exception for JSON errors.
         */
        public JSONObject asJSONObject() throws JSONException {
            return new JSONObject(asString());
        }
    }

    /**
     * Status code of the response.
     */
    public static final class Code {

        private final int statusCode;
        private final Range<Integer> validCodes = new Range<Integer>(100, 599);
        private final Range<Integer> serviceFailureCodes = new Range<Integer>(500, 599);
        private final Range<Integer> clientErrorCodes = new Range<Integer>(400, 499);
        private final Range<Integer> successCodes = new Range<Integer>(200, 299);

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
            return validCodes.contains(statusCode) ? statusCode : -1;
        }

        /**
         * Returns true if the status code is of range 5xx.
         * @return true if service failure occurred.
         */
        public boolean isServiceFailure() {
            return serviceFailureCodes.contains(statusCode);
        }

        /**
         * Returns true if the status code is of range 4xx.
         * @return true if client error occurred.
         */
        public boolean isClientError() {
            return clientErrorCodes.contains(statusCode);
        }

        /**
         * Returns true if the status code is of range 2xx.
         * @return true if response has success code.
         */
        public boolean isSucessful() {
            return successCodes.contains(statusCode);
        }
    }
}
