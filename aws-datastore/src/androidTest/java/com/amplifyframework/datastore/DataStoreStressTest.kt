package com.amplifyframework.datastore

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.ApiCategory
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.query.Page
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.appsync.AppSyncClient
import com.amplifyframework.datastore.appsync.SynchronousAppSync
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.testmodels.commentsblog.*
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.ModelAssert
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.sync.SynchronousApi
import com.amplifyframework.testutils.sync.SynchronousDataStore
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DataStoreStressTest {
    companion object {
        private const val TIMEOUT_SECONDS = 60
        private const val PAGINATION_LIMIT = 10
        private const val QUERY_WAIT = 2000L
        private lateinit var api: SynchronousApi
        private lateinit var appSync: SynchronousAppSync
        private lateinit var dataStore: SynchronousDataStore
        private lateinit var richard: BlogOwner
        private lateinit var richards: MutableList<BlogOwner>
        private lateinit var storageCategory: StorageCategory
        lateinit var synchronousDataStore: SynchronousDataStore
        private lateinit var modelProvider: ModelProvider

        @BeforeClass
        @JvmStatic
        fun setupOnce() {
            val context = getApplicationContext<Context>()
            @RawRes val configResourceId = Resources.getRawResourceId(context, "amplifyconfigurationupdated")

            // Setup an API
            val apiCategoryConfiguration = AmplifyConfiguration.fromConfigFile(context, configResourceId)
                .forCategoryType(CategoryType.API)
            val apiCategory = ApiCategory()
            apiCategory.addPlugin(AWSApiPlugin())
            apiCategory.configure(apiCategoryConfiguration, context)

            // To arrange and verify state, we need to access the supporting AppSync API
            api = SynchronousApi.delegatingTo(apiCategory)
            appSync = SynchronousAppSync.using(AppSyncClient.via(apiCategory))

            val tenMinutesAgo = Date().time - TimeUnit.MINUTES.toMillis(10)
            val tenMinutesAgoDateTime = Temporal.DateTime(Date(tenMinutesAgo), 0)
            val dataStoreCategory = DataStoreCategoryConfigurator.begin()
                .api(apiCategory)
                .clearDatabase(true)
                .context(context)
                .modelProvider(AmplifyModelProvider.getInstance())
                .resourceId(configResourceId)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .dataStoreConfiguration(DataStoreConfiguration.builder()
                    .syncExpression(BlogOwner::class.java) { BlogOwner.CREATED_AT.gt(tenMinutesAgoDateTime) }
                    .syncExpression(Blog::class.java) { Blog.CREATED_AT.gt(tenMinutesAgoDateTime) }
                    .syncExpression(Post::class.java) { Post.CREATED_AT.gt(tenMinutesAgoDateTime) }
                    .syncExpression(Comment::class.java) { Comment.CREATED_AT.gt(tenMinutesAgoDateTime) }
                    .syncExpression(Author::class.java) { Author.CREATED_AT.gt(tenMinutesAgoDateTime) }
                    .build())
                .finish()
            dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory)

            // Init Amplify
            try {
                Amplify.addPlugin(AWSDataStorePlugin.builder()
                    .modelProvider(AmplifyModelProvider.getInstance())
                    .build())
                Amplify.configure(getApplicationContext())
                Log.i("MyAmplifyApp", "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e("MyAmplifyApp", "Could not initialize Amplify", error)
            }
            Thread.sleep(5000)

            // Save a model 100 times to be queried later
            richards = mutableListOf()
            val saveLatch = CountDownLatch(20)
            val richardAccumulator = HubAccumulator.create(
                HubChannel.DATASTORE,
                20
            )
                .start()

            for (i in 1 .. 20) {
                richard = BlogOwner.builder()
                    .name("Richard$i")
                    .build()

                Amplify.DataStore.save(richard,
                    {
                        Log.i("MyAmplifyApp", "Saved a post: ${richard.name}")
                        saveLatch.countDown()
                    },
                    {
                        fail()
                    }
                )
                richards.add(richard)
                sleep(100)
            }
            saveLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            richardAccumulator.await(60, TimeUnit.SECONDS)
        }

        /**
         * Clear the DataStore after each test.  Without calling clear in between tests, all tests after the first will fail
         * with this error: android.database.sqlite.SQLiteReadOnlyDatabaseException: attempt to write a readonly database.
         * @throws DataStoreException On failure to clear DataStore.
         */
        @AfterClass
        @JvmStatic
        @Throws(DataStoreException::class)
        fun teardown() {
            try {
                dataStore.clear()
            } catch (error: Exception) {
                // ok to ignore since problem encountered during tear down of the test.
            }
        }
    }

    /**
     * Starting the plugin in local mode (no API plugin) works without freezing or crashing the calling thread.
     * @throws AmplifyException Not expected; on failure to configure of initialize plugin.
     */
    @Test
    @Throws(AmplifyException::class)
    fun testMultipleSave() {
        val localCharley = BlogOwner.builder()
            .name("Charley Crockett")
            .build()
        val modelName = BlogOwner::class.java.simpleName
        val publishedMutationsAccumulator = HubAccumulator.create(
            HubChannel.DATASTORE,
            DataStoreHubEventFilters.publicationOf(modelName, localCharley.id),
            1
        )
            .start()

        dataStore.save(localCharley)
        publishedMutationsAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val remoteCharley = api.get(BlogOwner::class.java, localCharley.id)

        assertEquals(localCharley.id, remoteCharley.id)
        assertEquals(localCharley.name, remoteCharley.name)
    }

    @Test
    fun testMultipleDelete() {

    }

    @Test
    fun testMultipleDelete_AfterMultipleSave() {
        val owner = BlogOwner.builder()
            .name("Jean")
            .build()
        val modelName = BlogOwner::class.java.simpleName

        val accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreHubEventFilters.publicationOf(modelName, owner.id), 2)
                .start()
        dataStore.save(owner)
        dataStore.delete(owner)

        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Verify that the owner is deleted from the local data store.
        // Note: Currently, default GraphQL resolvers do not filter records that have been deleted.
        // Therefore, calling api to get the item at this point would still succeed.
        assertThrows(NoSuchElementException::class.java) { dataStore[BlogOwner::class.java, owner.id] }
    }

    @Test
    fun testMultipleQuery() {
        val remote = mutableListOf<BlogOwner>()
        Amplify.DataStore.query(BlogOwner::class.java,
            { query ->
                while (query.hasNext()) {
                    val q = query.next()
                    remote.add(q)
                }
            },
            { fail() }
        )
        sleep(QUERY_WAIT)

        // Make sure all queries are present
        assertTrue(remote.size == richards.size)

    }

    @Test
    fun testMultipleQueryPredicate() {
        val remote = mutableListOf<BlogOwner>()
        Amplify.DataStore.query(BlogOwner::class.java,
            Where.matches(BlogOwner.NAME.contains("Richard1")),
            { query ->
                while (query.hasNext()) {
                    val q = query.next()
                    remote.add(q)
                }
            },
            { fail() }
        )
        sleep(QUERY_WAIT)

        // Make sure all queries are present
        assertTrue(remote.size == richards.size)


    }

    @Test
    fun testMultipleQuerySort() {
        val remote = mutableListOf<BlogOwner>()
        Amplify.DataStore.query(BlogOwner::class.java,
            Where.sorted(BlogOwner.NAME.ascending()),
            { queries ->
                while (queries.hasNext()) {
                    remote.add(queries.next())
                }
            },
            { fail() }
        )
        sleep(QUERY_WAIT)

        // Make sure all queries are present
        assertTrue(remote.size == richards.size)
    }

    @Test
    fun testMultipleQueryPagination() {
        val remote = mutableListOf<BlogOwner>()
        Amplify.DataStore.query(BlogOwner::class.java,
            Where.matchesAll().paginated(Page.startingAt(0).withLimit(PAGINATION_LIMIT)),
            { queries ->
                while (queries.hasNext()) {
                    remote.add(queries.next())
                }
            },
            { fail() }
        )
        sleep(QUERY_WAIT)

        // Make sure all queries are present
        assertTrue(remote.size == PAGINATION_LIMIT)
    }

    @Test
    fun testMultipleObserve_AfterMultipleStop() {

    }

    @Test
    fun testMultipleStart_AfterMultipleClear() {
        dataStore.clear()
        dataStore.start()
    }

    @Test
    fun testMultipleStart_AfterMultipleStop() {
        dataStore.stop()
        dataStore.start()
    }
}