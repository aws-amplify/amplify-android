import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.sdk.appsync.amplify.authorizers.AmplifyIamAuthorizer
import com.amazonaws.sdk.appsync.events.Events
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.test.R
import com.amazonaws.sdk.appsync.events.utils.getEventsConfig
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.BeforeClass
import org.junit.Test
import java.util.UUID

internal class EventsWebSocketClientAmplifyIamTests {
    private val eventsConfig = getEventsConfig(InstrumentationRegistry.getInstrumentation().targetContext)
    private val iamAuthorizer = AmplifyIamAuthorizer(eventsConfig.awsRegion)
    private val defaultChannel = "default/${UUID.randomUUID()}"
    private val events = Events(eventsConfig.url)

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(
                AmplifyOutputs.fromResource(R.raw.amplify_outputs),
                ApplicationProvider.getApplicationContext())
        }
    }

    @Test
    fun testPublishWithIam(): Unit = runTest {
        // Publish the message
        val webSocketClient = events.createWebSocketClient(iamAuthorizer, iamAuthorizer, iamAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected REST response
        (result is PublishResult.Response) shouldBe true
        (result as PublishResult.Response).apply {
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
            successfulEvents[0].apply {
                index shouldBe 0
            }
        }
    }
}

