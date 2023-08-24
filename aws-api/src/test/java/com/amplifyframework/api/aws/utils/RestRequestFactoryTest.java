/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the {@link RestRequestFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public final class RestRequestFactoryTest {
    /**
     * Test if we can create a valid URL.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURL() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);
        assertEquals("The url generated should match",
                "http://amplify-android.com/path/to/path",
                url.toString());
    }

    /**
     * Test if we can create a valid URL with already existing path.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURLWithPath() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com/beta",
                "/path/to/path",
                null);
        assertEquals("The url generated should match",
                "http://amplify-android.com/beta/path/to/path",
                url.toString());
    }

    /**
     * Test if path without leading slash is still valid.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURLWithPathWithoutLeadingSlash() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com/beta",
                "path",
                null);
        assertEquals("The url generated should match",
                "http://amplify-android.com/beta/path",
                url.toString());
    }

    /**
     * Test creating a valid URL with queries.
     *
     * @throws MalformedURLException Throws when the url is invalid.
     */
    @Test
    public void createValidURLWithQuery() throws MalformedURLException {
        Map<String, String> queries = new HashMap<>();
        queries.put("key1", "value1");
        queries.put("key2", "value2");
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                queries);
        assertEquals("The url generated should match",
                "http://amplify-android.com/path/to/path?key1=value1&key2=value2",
                url.toString());
    }

    /**
     * Test creating a valid URL with queries containing special chars.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURLWithQueryEncoded() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com/beta",
                "/path",
                Collections.singletonMap("phone_number", "+1234567890"));
        assertEquals("The url generated should match",
                "http://amplify-android.com/beta/path?phone_number=+1234567890",
                url.toString());
    }

    /**
     * Test creating a valid URL with path containing special chars.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURLWithPathEncoded() throws MalformedURLException {
        String email = "first+last@email.com";
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/users/" + email,
                Collections.emptyMap());
        assertEquals("The url generated should match",
                "http://amplify-android.com/users/first+last@email.com",
                url.toString());
    }

    /**
     * Test creating a valid URL containing special chars.
     *
     * @throws MalformedURLException Throws if the URL is invalid.
     */
    @Test
    public void createValidURLWithSpecialCharacters() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://cool+domain.com/+@$%^&*",
                "/+@$%^&*",
                Collections.singletonMap("+@#$%^&*", "+@#$%^&*"));
        assertEquals("The url generated should match",
                "http://cool+domain.com/+@$%^&*/+@$%^&*?+@#$%^&*=+@#$%^&*",
                url.toString());
    }

    /**
     * Test if exception is thrown on invalid URL.
     *
     * @throws MalformedURLException Should throw since the URL is invalid.
     */
    @Test(expected = MalformedURLException.class)
    public void createInvalidURL() throws MalformedURLException {
        RestRequestFactory.createURL("asd.com",
                "/path/to/path",
                null);
    }

    /**
     * Test creates a GET request.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createGetRequest() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);
        Request request = RestRequestFactory.createRequest(url,
                null,
                null,
                HttpMethod.GET);
        assertNotNull("Request should not be null", request);
    }

    /**
     * Test creates a DELETE request.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createDeleteRequestWithoutBody() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);
        Request request = RestRequestFactory.createRequest(url,
                null,
                null,
                HttpMethod.DELETE);
        assertNotNull("Request should not be null", request);
        assertEquals(HttpMethod.DELETE.name(), request.method());
    }

    /**
     * Test creates a DELETE request with a body.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createDeleteRequestWithBody() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);
        Request request = RestRequestFactory.createRequest(url,
                "{}".getBytes(),
                null,
                HttpMethod.DELETE);
        assertNotNull("Request should not be null", request);
        assertEquals(HttpMethod.DELETE.name(), request.method());
    }

    /**
     * Test creates a DELETE request with headers.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createDeleteRequestWithHeaders() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);

        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        Request request = RestRequestFactory.createRequest(url,
                null,
                headers,
                HttpMethod.DELETE);
        assertNotNull("Request should not be null", request);
        assertEquals("Header values should be set", "value1", request.header("key1"));
    }

    /**
     * Test creates a DELETE request with headers and body.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createDeleteRequestWithHeadersANDBody() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);

        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        Request request = RestRequestFactory.createRequest(url,
                "{}".getBytes(),
                headers,
                HttpMethod.DELETE);
        assertNotNull("Request should not be null", request);
        assertEquals("Header values should be set", "value1", request.header("key1"));
    }

    /**
     * Test creates a POST request.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createPostRequest() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);
        Request request = RestRequestFactory.createRequest(url,
                null,
                null,
                HttpMethod.POST);
        assertNotNull("Request should not be null", request);
    }

    /**
     * Test creates a POST request with a body.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createPostRequestWithBody() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);
        Request request = RestRequestFactory.createRequest(url,
                "{}".getBytes(),
                null,
                HttpMethod.POST);
        assertNotNull("Request should not be null", request);
    }

    /**
     * Test creates a POST request with headers.
     *
     * @throws MalformedURLException Throws when the URL is invalid.
     */
    @Test
    public void createPostRequestWithHeaders() throws MalformedURLException {
        URL url = RestRequestFactory.createURL("http://amplify-android.com",
                "/path/to/path",
                null);

        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        Request request = RestRequestFactory.createRequest(url,
                null,
                headers,
                HttpMethod.POST);
        assertNotNull("Request should not be null", request);
        assertEquals("Header values should be set", "value1", request.header("key1"));
    }
}
