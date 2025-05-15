<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="225">

---

# AWS AppSync Events for Android

These libraries allows you to connect [AWS AppSync Events](https://docs.aws.amazon.com/appsync/latest/eventapi/event-api-welcome.html).
AWS AppSync Events lets you create secure and performant serverless WebSocket APIs that can broadcast real-time event data to millions of subscribers, without you having to manage connections or resource scaling. With this feature, you can build multi-user features such as a collaborative document editors, chat apps, and live polling systems.

Learn more about AWS AppSync Events by visiting the [Developer Guide](https://docs.aws.amazon.com/appsync/latest/eventapi/event-api-welcome.html).

There are two libraries available:

- `aws-sdk-appsync-events`: This is a standalone library that allows developers to connect to AWS AppSync Events. It does not depend on Amplify, and instead leaves it to the application developer to supply tokens or signatures when using Owner or IAM-based authorization. This is the recommended library if your application does not already use Amplify.
- `ws-sdk-appsync-amplify`: This library depends on `Amplify Android`, and contains some glue classes to use Amplify to implement the authorizers for `aws-sdk-appsync-events`. This additional library is recommended if your application is already using Amplify, and you want to authorize calls With AWS Cognito User Pool tokens or IAM.

## Installation

Add the dependency you prefer to your `build.gradle.kts` file.

```kotlin
// Standalone AWS AppSync Events library that does not require Amplify
implementation("com.amazonaws:aws-sdk-appsync-events:1.0.0")

// Allows Amplify to Authorize Events calls through UserPool tokens or IAM
implementation("com.amazonaws:aws-sdk-appsync-amplify:1.0.0")
```

## Usage Guide

See [AWS AppSync Events for Android Documentation](https://docs.amplify.aws/android/build-a-backend/data/connect-event-api/) for a complete implementation guide.

## Contributing

- [CONTRIBUTING.md](../CONTRIBUTING.md)

## Security

See [CONTRIBUTING](../CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
