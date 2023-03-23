# Setup Push Notifications

1. Follow the instructions to setup an Android project: https://docs.amplify.aws/lib/project-setup/create-application/q/platform/android/
2. Add Firebase to the project by following the section "Set up your Backend": https://docs.amplify.aws/sdk/push-notifications/getting-started/q/platform/android/#set-up-your-backend
3. Add push notifications dependency and plugin to your project.

```groovy
implementation project(':aws-push-notifications-pinpoint')
```

```kotlin
Amplify.addPlugin(AWSPinpointPushNotificationsPlugin())
```

4. Run the app and send test message from pinpoint console. Refer https://docs.aws.amazon.com/pinpoint/latest/userguide/messages-mobile.html