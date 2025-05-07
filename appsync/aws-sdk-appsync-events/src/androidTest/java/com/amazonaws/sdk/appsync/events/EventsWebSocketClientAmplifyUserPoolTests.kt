import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.sdk.appsync.amplify.authorizers.AmplifyUserPoolAuthorizer
import com.amazonaws.sdk.appsync.events.Events
import com.amazonaws.sdk.appsync.events.data.EventsException
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.test.R
import com.amazonaws.sdk.appsync.events.utils.Credentials
import com.amazonaws.sdk.appsync.events.utils.getEventsConfig
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import io.kotest.matchers.shouldBe
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.BeforeClass
import org.junit.Test

internal class EventsWebSocketClientAmplifyUserPoolTests {
    private val eventsConfig = getEventsConfig(InstrumentationRegistry.getInstrumentation().targetContext)
    private val userPoolAuthorizer = AmplifyUserPoolAuthorizer()
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
    fun testFailedPublishWithUnauthenticatedUserPool(): Unit = runTest {
        // Publish the message
        val webSocketClient = events.createWebSocketClient(userPoolAuthorizer, userPoolAuthorizer, userPoolAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected response
        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).apply {
            error shouldBe EventsException(
                "An unknown error occurred",
                cause = AuthException("Token is null", "Token received but is null. Check if you are signed in")
            )
        }
    }

    @Test
    fun testPublishWithAuthenticatedUserPool(): Unit = runTest {
        val credentials = Credentials.load(InstrumentationRegistry.getInstrumentation().targetContext)
        com.amplifyframework.kotlin.core.Amplify.Auth.signIn(credentials.first, credentials.second)

        // Publish the message
        val webSocketClient = events.createWebSocketClient(userPoolAuthorizer, userPoolAuthorizer, userPoolAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected response
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
