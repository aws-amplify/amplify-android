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

package com.amplifyframework.api.aws.utils;

import com.amplifyframework.api.rest.HttpMethod;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Rest the RestOperationRequestUtils.
 */
public final class RestOperationRequestUtilsTest {

    /**
     * Test if we can create a valid URL.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURL() throws MalformedURLException {

        URL url = RestOperationRequestUtils.constructURL("http://amplify-android.com",
                "path/to/path",
                null);
        assertEquals("The url generated should match",
                "http://amplify-android.com/path%2Fto%2Fpath",
                url.toString());
    }

    /**
     * Test if we can create a valid URL with already existing path.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURLWithPath() throws MalformedURLException {

        URL url = RestOperationRequestUtils.constructURL("http://amplify-android.com/beta",
                "path/to/path",
                null);
        assertEquals("The url generated should match",
                "http://amplify-android.com/beta/path%2Fto%2Fpath",
                url.toString());
    }

    /**
     * Test creating a valid URL with queries.
     *
     * @throws MalformedURLException Throws when the url is invalid.
     */
    @Test
    public void createValidURLWithQuery() throws MalformedURLException {

        HashMap<String, String> queries = new HashMap<String, String>();
        queries.put("key1", "value1");
        queries.put("key2", "value2");
        URL url = RestOperationRequestUtils.constructURL("http://amplify-android.com",
                "path/to/path",
                queries);
        assertEquals("The url generated should match",
                "http://amplify-android.com/path%2Fto%2Fpath?key1=value1&key2=value2",
                url.toString());
    }

    /**
     * Test if exception is thrown on invalid URL.
     *
     * @throws MalformedURLException Should throw since the URL is invalid.
     */
    @Test(expected = MalformedURLException.class)
    public void createInValidURL() throws MalformedURLException {
        RestOperationRequestUtils.constructURL("asd.com",
                "path/to/path",
                null);
    }

    /**
     * Test creates a Get request.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createGetRequest() throws MalformedURLException {
        URL url = RestOperationRequestUtils.constructURL("http://amplify-android.com",
                "path/to/path",
                null);
        Request request = RestOperationRequestUtils.constructOKHTTPRequest(url,
                null,
                null,
                HttpMethod.GET);
        assertNotNull("Request should not be null", request);
    }

    /**
     * Test creates a Post request.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createPostRequest() throws MalformedURLException {
        URL url = RestOperationRequestUtils.constructURL("http://amplify-android.com",
                "path/to/path",
                null);
        Request request = RestOperationRequestUtils.constructOKHTTPRequest(url,
                null,
                null,
                HttpMethod.POST);
        assertNotNull("Request should not be null", request);
    }

    /**
     * Test creates a Post request with headers.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createPostRequestWithHeaders() throws MalformedURLException {
        URL url = RestOperationRequestUtils.constructURL("http://amplify-android.com",
                "path/to/path",
                null);

        final Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        Request request = RestOperationRequestUtils.constructOKHTTPRequest(url,
                null,
                headers,
                HttpMethod.POST);
        assertNotNull("Request should not be null", request);
        assertEquals("Header values should be set", "value1", request.header("key1"));
    }
}
