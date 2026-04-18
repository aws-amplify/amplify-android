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
package com.amplifyframework.firehose

import com.amplifyframework.recordcache.RecordCacheDatabaseException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException
import com.amplifyframework.recordcache.RecordCacheValidationException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class AmplifyFirehoseExceptionConversionTest {

    @Test
    fun `from should pass through AmplifyFirehoseException unchanged`() {
        val original = AmplifyFirehoseStorageException("msg", "suggestion")
        val result = AmplifyFirehoseException.from(original)
        result shouldBe original
    }

    @Test
    fun `from should convert RecordCacheValidationException to AmplifyFirehoseValidationException`() {
        val cause = RecordCacheValidationException("bad input", "fix it")
        val result = AmplifyFirehoseException.from(cause)
        result.shouldBeInstanceOf<AmplifyFirehoseValidationException>()
        result.message shouldBe "bad input"
        result.recoverySuggestion shouldBe "fix it"
        result.cause shouldBe cause
    }

    @Test
    fun `from should convert RecordCacheDatabaseException to AmplifyFirehoseStorageException`() {
        val cause = RecordCacheDatabaseException("db error", "retry")
        val result = AmplifyFirehoseException.from(cause)
        result.shouldBeInstanceOf<AmplifyFirehoseStorageException>()
        result.message shouldBe "db error"
        result.recoverySuggestion shouldBe "retry"
        result.cause shouldBe cause
    }

    @Test
    fun `from should convert RecordCacheLimitExceededException to AmplifyFirehoseLimitExceededException`() {
        val cause = RecordCacheLimitExceededException("cache full", "flush first")
        val result = AmplifyFirehoseException.from(cause)
        result.shouldBeInstanceOf<AmplifyFirehoseLimitExceededException>()
        result.message shouldBe "cache full"
        result.recoverySuggestion shouldBe "flush first"
        result.cause shouldBe cause
    }

    @Test
    fun `from should convert unknown Throwable to AmplifyFirehoseUnknownException`() {
        val cause = RuntimeException("something unexpected")
        val result = AmplifyFirehoseException.from(cause)
        result.shouldBeInstanceOf<AmplifyFirehoseUnknownException>()
        result.message shouldBe "something unexpected"
        result.cause shouldBe cause
    }

    @Test
    fun `from should handle unknown Throwable with null message`() {
        val cause = RuntimeException()
        val result = AmplifyFirehoseException.from(cause)
        result.shouldBeInstanceOf<AmplifyFirehoseUnknownException>()
        result.message shouldBe "An unknown error occurred"
        result.cause shouldBe cause
    }
}
