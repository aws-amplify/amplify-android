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
package com.amplifyframework.kinesis

import com.amplifyframework.recordcache.RecordCacheDatabaseException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException
import com.amplifyframework.recordcache.RecordCacheValidationException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class AmplifyKinesisExceptionConversionTest {

    @Test
    fun `from should pass through AmplifyKinesisException unchanged`() {
        val original = AmplifyKinesisStorageException("msg", "suggestion")
        val result = AmplifyKinesisException.from(original)
        result shouldBe original
    }

    @Test
    fun `from should convert RecordCacheValidationException to AmplifyKinesisValidationException`() {
        val cause = RecordCacheValidationException("bad input", "fix it")
        val result = AmplifyKinesisException.from(cause)
        result.shouldBeInstanceOf<AmplifyKinesisValidationException>()
        result.message shouldBe "bad input"
        result.recoverySuggestion shouldBe "fix it"
        result.cause shouldBe cause
    }

    @Test
    fun `from should convert RecordCacheDatabaseException to AmplifyKinesisStorageException`() {
        val cause = RecordCacheDatabaseException("db error", "retry")
        val result = AmplifyKinesisException.from(cause)
        result.shouldBeInstanceOf<AmplifyKinesisStorageException>()
        result.message shouldBe "db error"
        result.recoverySuggestion shouldBe "retry"
        result.cause shouldBe cause
    }

    @Test
    fun `from should convert RecordCacheLimitExceededException to AmplifyKinesisLimitExceededException`() {
        val cause = RecordCacheLimitExceededException("cache full", "flush first")
        val result = AmplifyKinesisException.from(cause)
        result.shouldBeInstanceOf<AmplifyKinesisLimitExceededException>()
        result.message shouldBe "cache full"
        result.recoverySuggestion shouldBe "flush first"
        result.cause shouldBe cause
    }

    @Test
    fun `from should convert unknown Throwable to AmplifyKinesisUnknownException`() {
        val cause = RuntimeException("something unexpected")
        val result = AmplifyKinesisException.from(cause)
        result.shouldBeInstanceOf<AmplifyKinesisUnknownException>()
        result.message shouldBe "something unexpected"
        result.cause shouldBe cause
    }

    @Test
    fun `from should handle unknown Throwable with null message`() {
        val cause = RuntimeException()
        val result = AmplifyKinesisException.from(cause)
        result.shouldBeInstanceOf<AmplifyKinesisUnknownException>()
        result.message shouldBe "An unknown error occurred"
        result.cause shouldBe cause
    }
}
