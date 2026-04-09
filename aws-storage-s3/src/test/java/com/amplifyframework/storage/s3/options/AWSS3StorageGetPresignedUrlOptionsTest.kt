/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3.options

import com.amplifyframework.storage.s3.StorageAccessMethod
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class AWSS3StorageGetPresignedUrlOptionsTest {

    @Test
    fun `default method is GET`() {
        val options = AWSS3StorageGetPresignedUrlOptions.defaultInstance()
        options.method shouldBe StorageAccessMethod.GET
    }

    @Test
    fun `builder sets method to PUT`() {
        val options = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.PUT)
            .build()
        options.method shouldBe StorageAccessMethod.PUT
    }

    @Test
    fun `builder sets method to GET explicitly`() {
        val options = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.GET)
            .build()
        options.method shouldBe StorageAccessMethod.GET
    }

    @Test
    fun `from copies method`() {
        val original = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.PUT)
            .setValidateObjectExistence(true)
            .build()
        val copy = AWSS3StorageGetPresignedUrlOptions.from(original).build()
        copy.method shouldBe StorageAccessMethod.PUT
        copy.validateObjectExistence shouldBe true
    }

    @Test
    fun `equals considers method`() {
        val optionsGet = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.GET)
            .build()
        val optionsPut = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.PUT)
            .build()
        val optionsGetCopy = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.GET)
            .build()

        optionsGet shouldNotBe optionsPut
        optionsGet shouldBe optionsGetCopy
    }

    @Test
    fun `hashCode differs for different methods`() {
        val optionsGet = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.GET)
            .build()
        val optionsPut = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.PUT)
            .build()

        optionsGet.hashCode() shouldNotBe optionsPut.hashCode()
    }

    @Test
    fun `toString includes method`() {
        val options = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.PUT)
            .build()
        options.toString() shouldContain "method=PUT"
    }

    @Test
    fun `builder chains method with other options`() {
        val options = AWSS3StorageGetPresignedUrlOptions.builder()
            .method(StorageAccessMethod.PUT)
            .setUseAccelerateEndpoint(true)
            .setValidateObjectExistence(false)
            .expires(300)
            .build()

        options.method shouldBe StorageAccessMethod.PUT
        options.useAccelerateEndpoint() shouldBe true
        options.validateObjectExistence shouldBe false
        options.expires shouldBe 300
    }
}
