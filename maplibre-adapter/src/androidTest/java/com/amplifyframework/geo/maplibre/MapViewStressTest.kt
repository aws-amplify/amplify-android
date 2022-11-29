package com.amplifyframework.geo.maplibre

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.testutils.junitcategories.StressTests
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.experimental.categories.Category
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Category(StressTests::class)
class MapViewStressTest {
    @get:Rule
    var rule = ActivityScenarioRule(MapViewTestActivity::class.java)
    private var geo: SynchronousGeo? = null

    /**
     * Set up test categories to be used for testing.
     */
    @Before
    fun setUpBeforeTest() {
        val geoPlugin = AWSLocationGeoPlugin()
        val geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        geo = SynchronousGeo.delegatingTo(geoCategory)
    }

    /**
     * Calls mapView.setStyle 50 times
     */
    @Test
    fun testMultipleSetStyle() = runBlockingSignedIn(rule) {
        repeat(50) {
            val mapStyle = suspendCoroutine { continuation ->
                rule.scenario.onActivity { activity ->
                    activity.mapView.addOnDidFailLoadingMapListener { error ->
                        continuation.resumeWithException(RuntimeException(error))
                    }
                    activity.mapView.setStyle { style ->
                        continuation.resume(style)
                    }
                }
            }
            Assert.assertNotNull(mapStyle)
        }
    }

    private fun <T> runBlockingSignedIn(
        rule: ActivityScenarioRule<MapViewTestActivity>,
        block: suspend CoroutineScope.() -> T
    ): T {
        return runBlocking(Dispatchers.Main) {
            rule.scenario.onActivity {
                signOutFromCognito() // first sign out to ensure we are in clean state
                signInWithCognito()
            }
            val result = block()
            rule.scenario.onActivity { signOutFromCognito() }
            result
        }
    }

    private fun signInWithCognito() {
        val (username, password) = Credentials.load(ApplicationProvider.getApplicationContext())
        val result = AmplifyWrapper.auth.signIn(username, password)
        println("SignIn complete: ${result.isSignedIn}")
    }

    private fun signOutFromCognito() {
        AmplifyWrapper.auth.signOut()
    }
}