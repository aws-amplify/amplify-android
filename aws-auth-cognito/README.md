## User Acceptance Testing Guide

0. [Setup a Facebook app](https://aws-amplify.github.io/docs/sdk/android/authentication#facebook-with-cognito-identity)
1. Setup a new Android Studio app project
2. Run `amplify init` on it
3. Run `amplify add auth`

```terminal
Do you want to use the default authentication and security configuration? Default configuration with Social Provider (Federation)
How do you want users to be able to sign in? Username
Do you want to configure advanced settings? No, I am done.
What domain name prefix you want us to create for you? (default)
Enter your redirect signin URI: myapp://callback/
? Do you want to add another redirect signin URI No
Enter your redirect signout URI: myapp://signout/
? Do you want to add another redirect signout URI No
Select the social providers you want to configure for your user pool: Facebook

You've opted to allow users to authenticate via Facebook.  If you haven't already, you'll need to go to https://developers.facebook.com and create an App ID.

Enter your Facebook App ID for your OAuth flow:  (your app id)
Enter your Facebook App Secret for your OAuth flow:  (your app secret)
```

4. amplify push
5. Follow the steps to [locally publish Amplify](https://github.com/aws-amplify/amplify-android#local-publishing-of-artifacts) + the Java 8 compatability step below it.
6. Add the following dependencies in your app build.gradle file:

```gradle
  implementation 'com.amplifyframework:core:master'
  implementation 'com.amplifyframework:aws-auth-cognito:master'
```

7. Follow step 1 from [Setup Amazon Cognito Hosted UI](https://aws-amplify.github.io/docs/sdk/android/authentication#setup-amazon-cognito-hosted-ui-in-android-app)
8. Add the following method to your MainActivity:

```java
@Override
protected void onResume() {
    super.onResume();
    Intent activityIntent = getIntent();
    if (activityIntent.getData() != null &&
            "myapp".equals(activityIntent.getData().getScheme())) {
        Amplify.Auth.handleSignInWithUIResponse(activityIntent);
    }
}
```

9. Create a MainApplication class with the following code:

```java
public class MainApplication extends Application {
    private String TAG = "AuthAcceptanceTesting";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getApplicationContext());
            Log.i(TAG, "Configured!");
        } catch(AmplifyException exception) {
            Log.e(TAG, "Failed to configure Amplify", exception);
        }
    }
}
```

10. Add `android:name=".MainApplication"` inside the application tag inside AndroidManifest.xml
11. Copy the contents of awsconfiguration.json into amplifyconfiguration.json like so:

```json
{
    "UserAgent": "aws-amplify-cli/2.0",
    "Version": "1.0",
    "auth": {
      "plugins": {
        "awsCognitoAuthPlugin": {
          <paste contents of awsconfiguration.json>
        }
    }
  }
}
```
12. HOOORRAYYY!!! You can now call Amplify.Auth methods inside the onCreate method of MainActivity.
