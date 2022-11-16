package com.amplifyframework.geo.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import org.json.JSONObject
import org.junit.*

class GeoStressTest {
    private var geo: SynchronousGeo? = null

    /**
     * Set up test categories to be used for testing.
     */
    @Before
    fun setUpBeforeTest() {
        // Auth plugin uses default configuration
        // Geo plugin uses above auth category to authenticate users
        val geoPlugin = AWSLocationGeoPlugin()
        val geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        geo = SynchronousGeo.delegatingTo(geoCategory)
    }

    @After
    fun tearDown() {
        signOutFromCognito()
    }

    /**
     * Tests that default map resource's style document can be fetched from
     * Amazon Location Service using [AWSLocationGeoPlugin.getMapStyleDescriptor].
     *
     * Fetched document must follow the specifications for Mapbox Style format.
     * Both "layers" and "sources" are critical information required for rendering
     * a map, so assert that both fields exist in the document.
     */
    @Test
    fun styleDescriptorLoadsProperly() {
        signInWithCognito()
        val style = geo?.getMapStyleDescriptor(GetMapStyleDescriptorOptions.defaults())
        Assert.assertNotNull(style)
        Assert.assertNotNull(style?.json)

        // assert that style document is aligned with specs
        // https://docs.mapbox.com/mapbox-gl-js/style-spec/
        val json = JSONObject(style!!.json)
        Assert.assertTrue(json.has("layers"))
        Assert.assertTrue(json.has("sources"))
    }

    /**
     * Tests that user must be authenticated in order to fetch map resource from
     * Amazon Location Service.
     *
     * @throws GeoException will be thrown due to service exception.
     */
    @Test(expected = GeoException::class)
    fun cannotFetchStyleWithoutAuth() {
        // should not be authorized to fetch map resource from Amazon Location Service
        geo?.getMapStyleDescriptor(GetMapStyleDescriptorOptions.defaults())
    }

    private fun signInWithCognito() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        auth?.signIn(username, password)
    }

    private fun signOutFromCognito() {
        auth?.signOut()
    }

    companion object {
        lateinit var auth: SynchronousAuth

        /**
         * Set up test categories to be used for testing.
         */
        @BeforeClass
        @JvmStatic
        fun setUp() {
            // Auth plugin uses default configuration
            auth =
                SynchronousAuth.delegatingToCognito(ApplicationProvider.getApplicationContext(), AWSCognitoAuthPlugin())
        }
    }
}