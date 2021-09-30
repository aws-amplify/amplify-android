/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import org.junit.Assert;
import org.junit.Test;

public class ModelComparatorTest {

    /***
     * Test Comparater compares strings and returns less than.
     */
    @Test
    public void comparatorComparesStringValuesAsLessThan() {
        ModelComparator<BlogOwner> subject = new ModelComparator<>(BlogOwner.NAME.ascending(),
                BlogOwner.class);
        int result = subject.compare(BlogOwner.builder().name("Jessica").build(),
                BlogOwner.builder().name("Monica").build());
        Assert.assertEquals(-1, result / Math.abs(result));
    }

    /***
     * Test Comparater compares strings and returns greater than.
     */
    @Test
    public void comparatorComparesStringValuesAsGreaterThan() {
        ModelComparator<BlogOwner> subject = new ModelComparator<>(BlogOwner.NAME.descending(),
                BlogOwner.class);
        int result = subject.compare(BlogOwner.builder().name("Monica").build(),
                BlogOwner.builder().name("Jessica").build());
        Assert.assertEquals(1, result / Math.abs(result));
    }

    /***
     * Test Comparater compares strings and returns equals.
     */
    @Test
    public void comparatorComparesStringValuesAsEquals() {
        ModelComparator<BlogOwner> subject = new ModelComparator<>(BlogOwner.NAME.descending(),
                BlogOwner.class);
        int result = subject.compare(BlogOwner.builder().name("Monica").build(),
                BlogOwner.builder().name("Monica").build());
        Assert.assertEquals(0, result);
    }

    /***
     * Test Comparater compares int and returns less than.
     */
    @Test
    public void comparatorComparesIntValuesAsLessThan() {
        ModelComparator<Post> subject = new ModelComparator<>(Post.RATING.descending(),
                Post.class);
        int result = subject.compare(Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(2).build(),
                Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(3).build());
        Assert.assertEquals(-1, result / Math.abs(result));
    }

    /***
     * Test Comparater compares int and returns greater than.
     */
    @Test
    public void comparatorComparesIntValuesAsGreaterThan() {
        ModelComparator<Post> subject = new ModelComparator<>(Post.RATING.descending(),
                Post.class);
        int result = subject.compare(Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(3).build(),
                Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(2).build());
        Assert.assertEquals(1, result / Math.abs(result));
    }

    /***
     * Test Comparater compares int and returns equal.
     */
    @Test
    public void comparatorComparesIntValuesAsEquals() {
        ModelComparator<Post> subject = new ModelComparator<>(Post.RATING.descending(),
                Post.class);
        int result = subject.compare(Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(3).build(),
                Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(3).build());
        Assert.assertEquals(0, result);
    }
}
