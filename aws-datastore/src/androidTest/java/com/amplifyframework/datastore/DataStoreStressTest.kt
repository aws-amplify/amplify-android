package com.amplifyframework.datastore

import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.model.query.Page
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.Blog
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testmodels.commentsblog.Post
import com.amplifyframework.testmodels.commentsblog.PostStatus
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class DataStoreStressTest {
    companion object {
        private const val TIMEOUT_SECONDS = 60
        private const val PAGINATION_LIMIT = 10
        private lateinit var blogOwners: MutableList<BlogOwner>

        @BeforeClass
        @JvmStatic
        fun setupOnce() {
            // Init Amplify
            try {
                Amplify.addPlugin(
                    AWSDataStorePlugin.builder()
                        .modelProvider(AmplifyModelProvider.getInstance())
                        .build()
                )
                Amplify.configure(getApplicationContext())
                Log.i("DataStoreStressTest", "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e("DataStoreStressTest", "Could not initialize Amplify", error)
            }
        }
    }

    /**
     * Clear the DataStore after each test.  Without calling clear in between tests, all tests after the first will fail
     * with this error: android.database.sqlite.SQLiteReadOnlyDatabaseException: attempt to write a readonly database.
     * @throws DataStoreException On failure to clear DataStore.
     */
    @After
    @Throws(DataStoreException::class)
    fun teardown() {
        try {
            val latch = CountDownLatch(1)
            Amplify.DataStore.clear(
                {
                    latch.countDown()
                    Log.i("DataStoreStressTest", "DataStore cleared")
                },
                {
                    latch.countDown()
                    Log.e("DataStoreStressTest", "Error clearing DataStore", it)
                }
            )
            latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        } catch (error: Exception) {
            // ok to ignore since problem encountered during tear down of the test.
        }
    }

    /**
     * Calls DataStore.save 30 times
     */
    @Test
    fun testMultipleSave() {
        val latch = CountDownLatch(30)
        repeat(30) {
            val saveLatch = CountDownLatch(1)
            val blogOwner: BlogOwner = BlogOwner.builder()
                .name("BlogOwner" + UUID.randomUUID().toString())
                .build()
            val blog: Blog = Blog.builder()
                .name("Blog" + UUID.randomUUID().toString())
                .owner(blogOwner)
                .build()
            val post: Post = Post.builder()
                .title("Post" + UUID.randomUUID().toString())
                .status(PostStatus.ACTIVE)
                .rating(1)
                .blog(blog)
                .build()
            Amplify.DataStore.save(
                blogOwner,
                {
                    Log.i("DataStoreStressTest", "Post saved")
                    Amplify.DataStore.save(
                        blog,
                        {
                            Log.i("DataStoreStressTest", "Blog saved")
                            Amplify.DataStore.save(
                                post,
                                {
                                    Log.i("DataStoreStressTest", "BlogOwner saved")
                                    latch.countDown()
                                },
                                { Log.e("DataStoreStressTest", "BlogOwner not saved", it) }
                            )
                        },
                        { Log.e("DataStoreStressTest", "Blog not saved", it) }
                    )
                },
                { Log.e("DataStoreStressTest", "Post not saved", it) }
            )
            saveLatch.await(1, TimeUnit.SECONDS)
        }

        assertTrue(latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
    }

    /**
     * Call DataStore.save, then call DataStore.delete ; 20 times
     */
    @Test
    fun testMultipleDelete_AfterMultipleSave() {
        val latch = CountDownLatch(20)

        repeat(20) {
            val saveLatch = CountDownLatch(1)
            val deleteLatch = CountDownLatch(1)
            val blogOwner: BlogOwner = BlogOwner.builder()
                .name("BlogOwner" + UUID.randomUUID().toString())
                .build()
            val blog: Blog = Blog.builder()
                .name("Blog" + UUID.randomUUID().toString())
                .owner(blogOwner)
                .build()
            val post: Post = Post.builder()
                .title("Post" + UUID.randomUUID().toString())
                .status(PostStatus.ACTIVE)
                .rating(1)
                .blog(blog)
                .build()
            Amplify.DataStore.save(
                blogOwner,
                {
                    Log.i("DataStoreStressTest", "Post saved")
                    Amplify.DataStore.save(
                        blog,
                        {
                            Log.i("DataStoreStressTest", "Blog saved")
                            Amplify.DataStore.save(
                                post,
                                {
                                    Log.i("DataStoreStressTest", "BlogOwner saved")
                                    latch.countDown()
                                },
                                { Log.e("DataStoreStressTest", "BlogOwner not saved", it) }
                            )
                        },
                        { Log.e("DataStoreStressTest", "Blog not saved", it) }
                    )
                },
                { Log.e("DataStoreStressTest", "Post not saved", it) }
            )
            saveLatch.await(1, TimeUnit.SECONDS)

            Amplify.DataStore.delete(
                blogOwner,
                {
                    Log.i("DataStoreStressTest", "Post deleted")
                    Amplify.DataStore.delete(
                        blog,
                        {
                            Log.i("DataStoreStressTest", "Blog deleted")
                            Amplify.DataStore.delete(
                                post,
                                {
                                    Log.i("DataStoreStressTest", "BlogOwner deleted")
                                    latch.countDown()
                                },
                                { Log.e("DataStoreStressTest", "BlogOwner not deleted", it) }
                            )
                        },
                        { Log.e("DataStoreStressTest", "Blog not deleted", it) }
                    )
                },
                { Log.e("DataStoreStressTest", "Post not deleted", it) }
            )
            deleteLatch.await(1, TimeUnit.SECONDS)
        }

        assertTrue(latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
    }

    /**
     * Call DataStore.query 10 times
     */
    @Test
    fun testMultipleQuery() {
        saveToLaterQuery()
        val remote = mutableListOf<BlogOwner>()
        repeat(10) {
            val queryLatch = CountDownLatch(1)
            Amplify.DataStore.query(
                BlogOwner::class.java,
                { query ->
                    while (query.hasNext()) {
                        val q = query.next()
                        remote.add(q)
                    }
                },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
            queryLatch.await(1, TimeUnit.SECONDS)

            // Make sure all queries are present
            assertTrue(remote.size == blogOwners.size)
            remote.clear()
        }
    }

    /**
     *
     * Call DataStore.query with a predicate 10 times
     */
    @Test
    fun testMultipleQueryPredicate() {
        saveToLaterQuery()
        val remote = mutableListOf<BlogOwner>()
        repeat(10) {
            val queryLatch = CountDownLatch(1)
            Amplify.DataStore.query(
                BlogOwner::class.java,
                Where.matches(BlogOwner.NAME.contains("BlogOwner")),
                { query ->
                    while (query.hasNext()) {
                        val q = query.next()
                        remote.add(q)
                    }
                },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
            queryLatch.await(1, TimeUnit.SECONDS)

            // Make sure all queries are present
            assertTrue(remote.size == blogOwners.size)
            remote.clear()
        }
    }

    /**
     * Call DataStore.query with sort 10 times
     */
    @Test
    fun testMultipleQuerySort() {
        saveToLaterQuery()
        val remote = mutableListOf<BlogOwner>()
        repeat(10) {
            val queryLatch = CountDownLatch(1)
            Amplify.DataStore.query(
                BlogOwner::class.java,
                Where.sorted(BlogOwner.NAME.ascending()),
                { queries ->
                    while (queries.hasNext()) {
                        remote.add(queries.next())
                    }
                },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
            queryLatch.await(1, TimeUnit.SECONDS)

            // Make sure all queries are present
            assertTrue(remote.size == blogOwners.size)
            remote.clear()
        }
    }

    /**
     * Call DataStore.query with pagination 10 times
     */
    @Test
    fun testMultipleQueryPagination() {
        saveToLaterQuery()
        val remote = mutableListOf<BlogOwner>()
        repeat(10) {
            val queryLatch = CountDownLatch(1)
            Amplify.DataStore.query(
                BlogOwner::class.java,
                Where.matchesAll().paginated(Page.startingAt(0).withLimit(PAGINATION_LIMIT)),
                { queries ->
                    while (queries.hasNext()) {
                        remote.add(queries.next())
                    }
                },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
            queryLatch.await(1, TimeUnit.SECONDS)

            // Make sure all queries are present
            assertTrue(remote.size == PAGINATION_LIMIT)
            remote.clear()
        }
    }

    /**
     * Call DataStore.stop, then call DataStore.observe ; 50 times
     */
    @Test
    fun testMultipleObserve_AfterMultipleStop() {
        val latch = CountDownLatch(100)
        repeat(50) {
            Amplify.DataStore.stop(
                { latch.countDown() },
                { Log.e("DataStoreStressTest", it.toString()) }
            )

            Amplify.DataStore.observe(
                BlogOwner::class.java,
                {
                    latch.countDown()
                    Log.i("DataStoreStressTest", "Observation began")
                },
                { Log.i("DataStoreStressTest", it.item().toString()) },
                { Log.e("DataStoreStressTest", "Observation failed", it) },
                { Log.i("DataStoreStressTest", "Observation complete") }
            )
        }

        assertTrue(latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
    }

    /**
     * Call DataStore.clear, then call DataStore.start ; 30 times
     */
    @Test
    fun testMultipleStart_AfterMultipleClear() {
        val latch = CountDownLatch(60)
        repeat(30) {
            val clearLatch = CountDownLatch(1)
            Amplify.DataStore.clear(
                { latch.countDown() },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
            clearLatch.await(1, TimeUnit.SECONDS)
            Amplify.DataStore.start(
                { latch.countDown() },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
        }
        assertTrue(latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
    }

    /**
     * Call DataStore.stop, then call DataStore.start ; 50 times
     */
    @Test
    fun testMultipleStart_AfterMultipleStop() {
        val latch = CountDownLatch(100)
        repeat(50) {
            Amplify.DataStore.stop(
                { latch.countDown() },
                { Log.e("DataStoreStressTest", it.toString()) }
            )

            Amplify.DataStore.start(
                { latch.countDown() },
                { Log.e("DataStoreStressTest", it.toString()) }
            )
        }
        assertTrue(latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
    }

    // Save a model 10 times to be queried later
    private fun saveToLaterQuery() {
        blogOwners = mutableListOf()
        val latch = CountDownLatch(10)

        repeat(10) {
            val saveLatch = CountDownLatch(1)
            val blogOwner: BlogOwner = BlogOwner.builder()
                .name("BlogOwner" + UUID.randomUUID().toString())
                .build()
            val blog: Blog = Blog.builder()
                .name("Blog" + UUID.randomUUID().toString())
                .owner(blogOwner)
                .build()
            val post: Post = Post.builder()
                .title("Post" + UUID.randomUUID().toString())
                .status(PostStatus.ACTIVE)
                .rating(1)
                .blog(blog)
                .build()
            Amplify.DataStore.save(
                blogOwner,
                {
                    Log.i("DataStoreStressTest", "Post saved")
                    Amplify.DataStore.save(
                        blog,
                        {
                            Log.i("DataStoreStressTest", "Blog saved")
                            Amplify.DataStore.save(
                                post,
                                {
                                    Log.i("DataStoreStressTest", "BlogOwner saved")
                                    saveLatch.countDown()
                                },
                                { Log.e("DataStoreStressTest", "BlogOwner not saved", it) }
                            )
                        },
                        { Log.e("DataStoreStressTest", "Blog not saved", it) }
                    )
                },
                { Log.e("DataStoreStressTest", "Post not saved", it) }
            )
            blogOwners.add(blogOwner)
            saveLatch.await(1, TimeUnit.SECONDS)
        }
        latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
    }
}
