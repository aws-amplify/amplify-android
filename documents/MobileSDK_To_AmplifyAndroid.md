# Migrating to Amplify Android From AWS Mobile SDK
[Amplify Android](https://github.com/aws-amplify/amplify-android) simplifies integrating AWS services into Android apps, making it easier to add features like authentication, data storage, and real-time updates without lots of code. This user-friendly interface not only cuts down on complexity but also speeds up the development process.
With Amplify Android, developers get tools that make setup straightforward, provide detailed documentation, and support advanced capabilities like offline data sync and GraphQL. This means you can spend less time dealing with the technicalities of cloud integration and more time crafting engaging user experiences.
Choosing Amplify Android could lead to quicker development, plus applications that are both scalable and secure.

## General Migration Notes

- **Amplify Android** is the recommended library for all new Android related development.
- For any AWS service not yet supported by Amplify, you can use the [AWS SDK for Kotlin](https://github.com/awslabs/aws-sdk-kotlin) or reference [Kotlin SDK code examples](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/kotlin).
- Amplify will make a best-effort attempt to preserve user auth sessions during migration, but some users may need to re-authenticate.

## Categories

### [Authentication](https://docs.amplify.aws/android/build-a-backend/auth/set-up-auth/#set-up-backend-resources)

| AWS SDK For Android                                                                                                     | Amplify Android                                                                                                                                                                                           |
|-------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [SignUp](https://docs.amplify.aws/android/sdk/auth/working-with-api/#signup)                                            | [Sign Up]([https://docs.amplify.aws/android/sdk/auth/working-with-api/#signup](https://docs.amplify.aws/android/build-a-backend/auth/enable-sign-in/#register-a-user))                                                                                                                             |
| [Confirm SignUp](https://docs.amplify.aws/android/sdk/auth/working-with-api/#confirm-signup)                            | [Confirm SignUp](https://docs.amplify.aws/android/build-a-backend/auth/enable-sign-in/#register-a-user)                                                                                                   |
| [Sign In](https://docs.amplify.aws/android/sdk/auth/working-with-api/#signin)                                           | [Sign In](https://docs.amplify.aws/android/build-a-backend/auth/enable-sign-in/#sign-in-a-user)                                                                                                           |
| [Guest Access](https://docs.amplify.aws/android/sdk/auth/guest-access/)                                                 | [Guest Access](https://docs.amplify.aws/android/build-a-backend/auth/enable-guest-access/)                                                                                                                |
| [Drop-in Auth](https://docs.amplify.aws/android/sdk/auth/drop-in-auth/)                                                 | [Amplify UI Authenticator](https://ui.docs.amplify.aws/android/connected-components/authenticator/configuration)                                                                                          |
| [Confirm Sign In (MFA)](https://docs.amplify.aws/android/sdk/auth/working-with-api/#confirm-signin-mfa)                 | [MFA](https://docs.amplify.aws/android/build-a-backend/auth/enable-sign-in/#multi-factor-authentication)                                                                                                  |
| [Change Password](https://docs.amplify.aws/android/sdk/auth/working-with-api/#force-change-password)                    | [Change Password](https://docs.amplify.aws/android/build-a-backend/auth/manage-passwords/#change-password)                                                                                                |
| [Forgot Password](https://docs.amplify.aws/android/sdk/auth/working-with-api/#forgot-password)                          | [Reset Password](https://docs.amplify.aws/gen1/android/build-a-backend/auth/multi-step-sign-in/#reset-password)                                                                                                |
| [Managing Tokens and Credentials](https://docs.amplify.aws/android/sdk/auth/working-with-api/#managing-security-tokens) | [Accessing Credentials](https://docs.amplify.aws/android/build-a-backend/auth/accessing-credentials/)   |
| [SignOut](https://docs.amplify.aws/android/sdk/auth/working-with-api/#signout)                                          | [Sign Out](https://docs.amplify.aws/android/build-a-backend/auth/sign-out/)                                                                                                                               |
| [Global SignOut](https://docs.amplify.aws/android/sdk/auth/working-with-api/#global-signout)                            | [Global Sign Out](https://docs.amplify.aws/android/build-a-backend/auth/sign-out/#global-sign-out)                                                                                                        |
| [Federated Identities](https://docs.amplify.aws/android/sdk/auth/federated-identities/)                                 | [Federated Identities](https://docs.amplify.aws/android/build-a-backend/auth/advanced-workflows/#identity-pool-federation)                                                                                |
| [Hosted UI](https://docs.amplify.aws/android/sdk/auth/hosted-ui/#using-auth0-hosted-ui)                                 | [Web UI Sign In](https://docs.amplify.aws/android/build-a-backend/auth/sign-in-with-web-ui/) / [Social Web UI Sign In](https://docs.amplify.aws/android/build-a-backend/auth/add-social-provider/)        |
| [Custom Auth Flow](https://docs.amplify.aws/android/sdk/auth/custom-auth-flow/)                                         | [Custom Auth Flow](https://docs.amplify.aws/android/build-a-backend/auth/sign-in-custom-flow/#configure-auth-category)                                                                                    |
| [Track/Remember Device](https://docs.amplify.aws/android/sdk/auth/device-features/)                                     | [Track/Remember Device](https://docs.amplify.aws/android/build-a-backend/auth/remember-device/#configure-auth-category)                                                                                   |


Notes:

* Drop-in Auth provided by `AWSMobileClient` is replaced with [Authenticator UI component](https://ui.docs.amplify.aws/android/connected-components/authenticator). Additionally, Authenticator UI component does not support social providers yet. You can track updates on it [here](https://github.com/aws-amplify/amplify-ui-android/issues/125).

* To manually refresh the ID and access tokens (such as in scenarios where you might need to ensure that you have the latest tokens, perhaps because of changes in permissions or other security-related updates) refer to [Amplify docs](_https://docs.amplify.aws/android/build-a-backend/auth/accessing-credentials/#force-refreshing-session_)

* Amplify Sign In's will handle token refreshing as long as the refresh token is valid. Amplify does not support refreshing tokens automatically through `federateToIdentityPool()`.

### [Storage](https://docs.amplify.aws/android/build-a-backend/storage/configure-storage/#setup-a-new-storage-resource)

| AWS SDK For Android                                                                                                              | Amplify Android                                                                                                           |
|----------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| [Upload File](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#upload-a-file)                                      | [Upload File](https://docs.amplify.aws/android/build-a-backend/storage/upload/#upload-files)                              |
| [Download File](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#download-a-file)                                  | [Download File](https://docs.amplify.aws/android/build-a-backend/storage/download/#download-to-file)                      |
| [Track Progress](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#track-transfer-progress)                         | [Track Progress Download](https://docs.amplify.aws/android/build-a-backend/storage/download/#track-download-progress)     |
| [Pause/Resume/Start/Cancel](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#pause-a-transfer)                     | [Pause/Resume/Start/Cancel](https://docs.amplify.aws/android/build-a-backend/storage/query-transfers/)                    |
| [Long-running Transfers](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#long-running-transfers)                  | Amplify supports this by default                                                                                          |
| [Transfer with Metadata](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#transfer-with-object-metadata)           | [Transfer with Metadata](https://docs.amplify.aws/android/build-a-backend/storage/upload/#transfer-with-object-metadata)  |
| [Transfer Network Connection Type](https://docs.amplify.aws/android/sdk/storage/transfer-utility/#transfernetworkconnectiontype) | Amplify does not support this for now                                                                                     |


### [REST API](https://docs.amplify.aws/android/build-a-backend/restapi/configure-rest-api/#create-a-rest-api)

| AWS SDK For Android                                                                                                | Amplify Android                                                                                                                                                                                                                                                                                                                                          |
|--------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Basic Operations](https://docs.amplify.aws/android/sdk/api/rest/)                                                 | [Create](https://docs.amplify.aws/android/build-a-backend/restapi/set-up-rest-api/#make-a-post-request), [Fetch](https://docs.amplify.aws/android/build-a-backend/restapi/fetch-data/), [Update](https://docs.amplify.aws/android/build-a-backend/restapi/update-data/), [Delete](https://docs.amplify.aws/android/build-a-backend/restapi/delete-data/) |
| [IAM Authorization](https://docs.amplify.aws/android/sdk/api/rest/#iam-authorization)                              | [IAM Authorization](https://docs.amplify.aws/android/build-a-backend/restapi/customize-authz/)                                                                                                                                                                                                                                                           |
| [Cognito User pool Authorization](https://docs.amplify.aws/android/sdk/api/rest/#cognito-user-pools-authorization) | [Cognito User pool Authorization](https://docs.amplify.aws/android/build-a-backend/restapi/customize-authz/#cognito-user-pool-authorization)                                                                                                                                                                                                             |

### [Push Notification](https://docs.amplify.aws/android/build-a-backend/push-notifications/set-up-push-notifications/#set-up-backend-resources)

> **Pinpoint deprecation notice** – Pinpoint will be retired Oct 30 2026. AWS End User Messaging is the recommended successor. Plan migrations accordingly.

| AWS SDK For Android                                                                                                                                                                                 | Amplify Android                                                                                                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Setup](https://docs.amplify.aws/android/sdk/push-notifications/getting-started/), [Push Notification Service Setup](https://docs.amplify.aws/android/sdk/push-notifications/messaging-campaign/)   | [Setup](https://docs.amplify.aws/android/build-a-backend/push-notifications/set-up-push-notifications/), [Push Notification Service Setup](https://docs.amplify.aws/android/build-a-backend/push-notifications/set-up-push-service/)  |
| [Register Device](https://docs.amplify.aws/android/sdk/push-notifications/messaging-campaign/)                                                                                                      | [Register Device](https://docs.amplify.aws/android/build-a-backend/push-notifications/register-device/)                                                                                                                               |
| [Record Notification Events](https://docs.amplify.aws/android/sdk/push-notifications/messaging-campaign/)                                                                                           | [Record Notification Events](https://docs.amplify.aws/android/build-a-backend/push-notifications/record-notifications/)                                                                                                               |

### [Analytics](https://docs.amplify.aws/android/build-a-backend/more-features/analytics/set-up-analytics/#set-up-analytics-backend)

| AWS SDK For Android                                                                                     | Amplify Android                                                                                                                          |
|---------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| [Manually Track Session](https://docs.amplify.aws/android/sdk/analytics/getting-started/#add-analytics) | [Automatically Track Session](https://docs.amplify.aws/android/sdk/analytics/getting-started/#add-analytics)                             |
| [Add Analytics](https://docs.amplify.aws/android/sdk/analytics/getting-started/#add-analytics)          | [Add Analytics](https://docs.amplify.aws/android/build-a-backend/more-features/analytics/set-up-analytics/#initialize-amplify-analytics) |
| [Authentication Events](https://docs.amplify.aws/android/sdk/analytics/events/#session-events)          | [Authentication events](https://docs.amplify.aws/android/build-a-backend/more-features/analytics/record-events/#authentication-events)   |
| [Custom Events](https://docs.amplify.aws/android/sdk/analytics/events/#custom-events)                   | [Custom Events](https://docs.amplify.aws/android/build-a-backend/more-features/analytics/record-events/)                                 |
| [Monetization events](https://docs.amplify.aws/android/sdk/analytics/events/#monetization-events)       | Please Check Notes                                                                                                                       |

Notes:

* For recording events:

When migrating from the AWS SDK for Android to Amplify, you'll need to adjust your approach to handle user sessions, as the two libraries have different mechanisms for managing user authentication and session management.

* Use `Amplify.Analytics.recordEvent()` for recording Monetization event.

```
// Example:    
    val PURCHASE_EVENT_NAME = "_monetization.purchase"
    val PURCHASE_EVENT_QUANTITY_METRIC = "_quantity"
    val PURCHASE_EVENT_ITEM_PRICE_METRIC = "_item_price"
    val PURCHASE_EVENT_PRODUCT_ID_ATTR = "_product_id"
    val PURCHASE_EVENT_PRICE_FORMATTED_ATTR = "_item_price_formatted"
    val PURCHASE_EVENT_STORE_ATTR = "_store"
    val PURCHASE_EVENT_TRANSACTION_ID_ATTR = "_transaction_id"
    val PURCHASE_EVENT_CURRENCY_ATTR = "_currency"
    
    val event = AnalyticsEvent.builder()
            .name(PURCHASE_EVENT_NAME)
            .addProperty(PURCHASE_EVENT_PRODUCT_ID_ATTR, productId)
            .addProperty(PURCHASE_EVENT_STORE_ATTR, store)
            .addProperty(PURCHASE_EVENT_QUANTITY_METRIC, quantity)
            .addProperty(PURCHASE_EVENT_PRICE_FORMATTED_ATTR, formattedItemPrice)
            .addProperty(PURCHASE_EVENT_ITEM_PRICE_METRIC, itemPrice)
            .addProperty(PURCHASE_EVENT_TRANSACTION_ID_ATTR, transactionId)
            .addProperty(PURCHASE_EVENT_CURRENCY_ATTR, currency)
            .build()
    Amplify.Analytics.recordEvent(event)
```

* Amplify Analytics events have default configuration to flush out to the network every 30 seconds. This interval can be configured in `amplifyconfiguration.json.` Refer [Flush Events](https://docs.amplify.aws/android/build-a-backend/more-features/analytics/record-events/#flush-events) section in Amplify Documentation for more details.
* Limits applicable to ingestion of events for Amazon Pinpoint can be found [here](https://docs.aws.amazon.com/pinpoint/latest/developerguide/quotas.html#limits-events).

### [AWS IoT](https://docs.amplify.aws/gen1/android/sdk/pubsub/getting-started/#aws-iot)

Notes:

* For AWS IoT, we recommend using [AWS SDK for JAVA](https://github.com/aws/aws-iot-device-sdk-java)

# <Stop Reading>

