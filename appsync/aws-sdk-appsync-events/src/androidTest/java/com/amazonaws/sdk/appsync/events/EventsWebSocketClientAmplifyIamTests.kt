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
import com.amplifyframework.testutils.coroutines.runBlockingWithTimeout
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID
import kotlinx.serialization.json.JsonPrimitive
import org.junit.BeforeClass
import org.junit.Test

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
                ApplicationProvider.getApplicationContext()
            )
        }
    }

    @Test
    fun testPublishWithIam(): Unit = runBlockingWithTimeout {
        // Publish the message
        val webSocketClient = events.createWebSocketClient(iamAuthorizer, iamAuthorizer, iamAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected REST response
        val response = result.shouldBeInstanceOf<PublishResult.Response>()
        response.failedEvents.shouldBeEmpty()
        response.successfulEvents.shouldHaveSize(1)
        response.successfulEvents.first().index shouldBe 0
    }
}
