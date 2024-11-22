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
implementation("com.amplifyframework:apollo-appsync:1.1.0")

// To connect Apollo to AppSync using your existing Amplify Gen2 Backend
implementation("com.amplifyframework:apollo-appsync-amplify:1.1.0")
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
    .appSync(connector.endpoint, connector.apiKeyAuthorizer())
    .build()
```

Once you have constructed the Apollo client you can use it as normal for queries, mutations, and subscriptions to AppSync.

## Authorization Modes

AWS AppSync supports [five different authorization modes](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html):

- API Key
- AWS Lambda Function
- AWS IAM Permissions
- OIDC Provider
- Amazon Cognito User Pool

The Apollo AppSync Extensions libraries expose three authorizer types to support these different authorization modes.

### ApiKeyAuthorizer

An `ApiKeyAuthorizer` is used to provide a key for [API Key authorization](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html#api-key-authorization) requests.

This Authorizer can be used with a hardcoded API key, by fetching the key from some source, or reading it from `amplify_outputs.json`:

```kotlin
// Create an authorizer directly with your API key:
val authorizer = ApiKeyAuthorizer("[YOUR_API_KEY")
```
```kotlin
// Create an authorizer that fetches your API key. The fetching function may be called many times, 
// and should internally implement an appropriate caching mechanism.
val authorizer = ApiKeyAuthorizer { fetchApiKey() }
```
```kotlin
// Using ApolloAmplifyConnector to read API key from amplify_outputs.json
val connector = ApolloAmplifyConnector(context, AmplifyOutputs(R.raw.amplify_outputs))
val authorizer = connector.apiKeyAuthorizer()
```

### AuthTokenAuthorizer

An `AuthTokenAuthorizer` sets an authentication header for use with [AWS Lambda](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html#aws-lambda-authorization), 
[OIDC provider](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html#openid-connect-authorization), and
[Amazon Cognito User Pool](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html#amazon-cognito-user-pools-authorization)
authorization modes.

Using `ApolloAmplifyConnector` allows you to automatically authorize requests for the signed-in Amplify user, or you can implement the Authorizer's function parameter yourself to provide other types of tokens.

```kotlin
// Provide a token from e.g. an OIDC provider. The fetching function may be called many times, 
// and should internally implement an appropriate caching mechanism.
val authorizer = AuthTokenAuthorizer { fetchUserToken() }
```
```kotlin
// Use ApolloAmplifyConnector to get Cognito tokens from Amplify for the signed-in user
val connector = ApolloAmplifyConnector(context, AmplifyOutputs(R.raw.amplify_outputs))
val authorizer = connector.authTokenAuthorizer()
// or
val authorizer = AuthTokenAuthorizer { ApolloAmplifyConnector.fetchLatestCognitoAuthToken() }
```

### IamAuthorizer

An `IamAuthorizer` is used to provide request signature headers for using [AWS IAM-based authorization](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html#aws-iam-authorization).

Using `ApolloAmplifyConnector` is the easiest way to use this authorizer, but you can also implement the signing function yourself, by e.g. delegating to the [AWS Kotlin SDK](https://github.com/awslabs/aws-sdk-kotlin).

```kotlin
// Provide an implementation of the signing function. This function should implement the 
// AWS Sig-v4 signing logic and return the authorization headers containing the token and signature.
val authorizer = IamAuthorizer { signRequestAndReturnHeaders(it) }
```
```kotlin
// Use ApolloAmplifyConnector to sign the request
val connector = ApolloAmplifyConnector(context, AmplifyOutputs(R.raw.amplify_outputs))
val authorizer = connector.iamAuthorizer()
// or supply a region to sign via the companion function
val authorizer = IamAuthorizer { ApolloAmplifyConnector.signAppSyncRequest(it, "us-east-1") }
```

## Contributing

- [CONTRIBUTING.md](../CONTRIBUTING.md)

## Security

See [CONTRIBUTING](../CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
