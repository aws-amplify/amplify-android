<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="225">

---

# AWS AppSync Apollo Extensions for Kotlin

These libraries allows you to connect [Apollo Kotlin](https://www.apollographql.com/docs/kotlin/) v4.x to [AWS AppSync](https://aws.amazon.com/pm/appsync/).

There are two libraries available:

- `apollo-appsync`: This library implements the authorization and protocol logic for Apollo to connect to AppSync. It does not depend on Amplify, and instead leaves it to the application developer to supply tokens or signatures when using Owner or IAM-based authorization. This is the recommended library if your application does not already use Amplify.
- `apollo-appsync-amplify`: This library depends on both `apollo-appsync` and `Amplify Android`, and contains some glue classes to use Amplify to implement the authorizers for `apollo-appsync`. This is the recommended library if your application is already using Amplify.

## Usage

Add the dependency you prefer to your `build.gradle.kts` file.

```kotlin
// To only use Apollo to speak to AppSync, without using Amplify
implementation("com.amplifyframework:apollo-appsync:1.0.0")

// To connect Apollo to AppSync using your existing Amplify Gen2 Backend
implementation("com.amplifyframework:apollo-appsync-amplify:1.0.0")
```

For applications using `apollo-appsync` directly, instantiate the Endpoint and the desired Authorizer instance, and then call the Apollo builder extension.

```kotlin
val endpoint = AppSyncEndpoint("https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql")
val authorizer = ApiKeyAuthorizer("[YOUR_API_KEY")

val apolloClient = ApolloClient.Builder()
    .appSync(endpoint, authorizer)
    .build()
```

For applications using `apollo-appsync-amplify`, you can connect directly to your Amplify Gen2 Backend using an `ApolloAmplifyConnector`. This class can create Authorizer instances that use Amplify to provide Cognito Tokens and sign requests as needed.

```kotlin
val connector = ApolloAmplifyConnector(context, AmplifyOutputs(R.raw.amplify_outputs))

val apolloClient = ApolloClient.Builder()
    .appSync(connector.endpoint, connect.apiKeyAuthorizer())
    .build()
```

Once you have constructed the Apollo client you can use it as normal for queries, mutations, and subscriptions to AppSync.

## Contributing

- [CONTRIBUTING.md](../CONTRIBUTING.md)

## Security

See [CONTRIBUTING](../CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.