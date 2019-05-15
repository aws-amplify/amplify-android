{% include header.html %}
# AWS Amplify Auth

## Setup

Add gradle dependencies to begin using the library.

```groovy
// Required as a base for Auth
implementation 'com.amazonaws:aws-amplify-auth:{{ site.sdk_version }}'
```

Add configuration in `res/raw/awsconfiguration.json`.
Any combination of IdentityPool, UserPool, AccessKey, and Google maybe used;
however, functionality will be limited by the combination chosen.

```
{
  "Version": "1.0",
  "Auth": {
    "Default": {
      "IdentityPool": {
        "PoolId": "us-west-2:asdf123-asdf-1234-asdf-1234asdf1234",
        "Region": "us-west-2"
      },
      "UserPool": {
        "PoolId": "us-west-2_asdfasdf",
        "ClientId": "7j1unppasdfasdfasdfasdf",
        "ClientSecret": "asdfasdfasdfasdfasdfasdfasdfcd59492irr1ui",
        "Region": "us-west-2"
      },
      "AccessKey": {
        "AccessKey": "asdfasdfasdf",
        "SecretKey": "asdfasdfasdfasdfasdfasdfasdf"
      }
      "Google": {
        "ClientId": "asdfasdfasdf"
      }
    }
  }
}
```

## Create a client

{% include tabs.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
val auth = Auth(applicationContext)
```

{% include tab_content_end.html %}

{% include tab_content_start.html lang="java" %}

```java
Auth auth = new Auth(MainActivity.getApplicationContext());
```

{% include tab_content_end.html %}

## Sign-in

1. Call `signIn`.

{% include tabs.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
auth.signIn("bimin", "1234Password!", validationData, object: Callback<UserpoolSignInResult>() {
    override fun onResult(result: UserpoolSignInResult) {
        when (result.signInState) {
            SignInState.SMS_MFA -> // Request MFA code from user, call auth.confirmSignIn(...)
            SignInState.NEW_PASSWORD -> // Request new password from user, call auth.confirmSignIn(...)
            SignInState.DONE -> // Handle completed sign-in
        }
    }

    override fun onError(error: Exception) {
        // Handle error
    }
})
```

{% include tab_content_end.html %}

{% include tab_content_start.html lang="java" %}

```java
auth.signIn("bimin", "1234Password!", validationData, new Callback<UserpoolSignInResult>() {
    @Override
    void onResult(final UserpoolSignInResult result) {
        switch (result.getSignInState()) {
            case: SignInState.SMS_MFA: // Request MFA code from user, call auth.confirmSignIn(...)
            case: SignInState.NEW_PASSWORD: // Request new password from user, call auth.confirmSignIn(...)
            case: SignInState.DONE: // Handle completed sign-in
        }
    }

    @Override
    void onError(final Execption e) {
        // Handle error
    }
});
```

{% include tab_content_end.html %}

### Identity Id

The identity id is associated with the user and the identity provider they used.
This id can be used to store and identify user resources such as an S3 folder with the id as the name and contents belonging to that user `user-abcd-123/attachment.jpg`.

{% include tabs.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
auth.identityId
```

{% include tab_content_end.html %}

{% include tab_content_start.html lang="java" %}

```java
auth.getIdentityId();
```

{% include tab_content_end.html %}

Side notes: If `person A` signs in with Userpool and gets identity id `abcd-123`.
However, next time `person A` signs in with Google will get identity id `efgh-456` **unless the identity is merged**.

## Sign-up

{% include tabs.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
userpool.signUp("bimin", "1234Password!", object: Callback<UserpoolSignUpResult>() {
    override fun onResult(result: UserpoolSignUpResult) {
        when (result.signUpState) {
            SignUpState.SMS_MFA -> // Request MFA code from user, call auth.confirmSignUp(..)
            SignUpState.DONE -> // Handle completed sign-in
        }
    }

    override fun onError(error: Exception) {
        // Handle error
    }
})
```

{% include tab_content_end.html %}

{% include tab_content_start.html lang="java" %}

```java
auth.signUp("bimin", "1234Password!", new Callback<UserpoolSignUpResult>() {
    @Override
    public void onResult(UserpoolSignUpResult result) {
        switch (result.getSignUpState()) {
            case: SignUpState.SMS_MFA: // Request MFA code from user, call auth.confirmSignUp(..)
            case: SignUpState.DONE: // Handle completed sign-in
        }
    }

    @Override
    void onError(final Execption e) {
        // Handle error
    }
})
```

{% include tab_content_end.html %}

## API Reference

[Show me the APIs](/reference)

{% include footer.html %}
