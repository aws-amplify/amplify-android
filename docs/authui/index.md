{% include header.html %}
# AWS Amplify Auth UI

## Setup

Add gradle dependencies to begin using the library.
    
```groovy
// Required as a base for AuthUI
implementation 'com.amazonaws:aws-amplify-authui:{{ site.sdk_version }}'
```

## Prerequisites
Add awsconfiguration.json to the res/raw folder of the app module. The file must contain aws credentials needed for
authentication.
Sample awsconfiguration.json : 

```json
{
  "Version": "1.0",
  "Auth" : {
    "Default": {
      "IdentityPool": {
        "PoolId": "us-east-1:feb48986-c28c-4d0e-b042-aa17xxxxxxxx",
        "Region": "us-east-1"
      },
      "UserPool": {
        "PoolId": "us-east-1_RPWAxxxxx",
        "ClientId": "5ekh0ive9vk733kgp3xxxxxxxx",
        "ClientSecret": "1bqe5l3jc6dnn0ub2tr0d357a5stdq5q8m8s7cm3q0v6xxxxxxxx",
        "Region": "us-east-1"
      }
    }
  }
}
```

## Instantiating AuthUI

{% include tabs.html %}

{% include tab_content_start.html lang="java" %}

```java
AuthUI authUI = new AuthUI(CallingActivity.this);
```

{% include tab_content_end.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
val authUI = AuthUI(this@CallingActivity)
```

{% include tab_content_end.html %}

## Sign-in

{% include tabs.html %}

{% include tab_content_start.html lang="java" %}

```java
        authUI.signIn(new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                Intent intent = new Intent(context, NextActivity.class);
                context.startActivity(intent);
            }

            @Override
            public void onError(Exception e) {
                Log.e(LOG_TAG, "Error Signing In : ", e);
            }
        });
```
{% include tab_content_end.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
authUI.signIn(object : Callback<Void>() {
    fun onResult(result: Void) {
        val intent = Intent(context, NextActivity::class.java)
        context.startActivity(intent)
    }

    fun onError(e: Exception) {
        Log.e(LOG_TAG, "Error Signing In : ", e)
    }
})
```

{% include tab_content_end.html %}

## Sign-out

{% include tabs.html %}

{% include tab_content_start.html lang="java" %}

```java
    authUI.signOut(new Callback<Void>() {
        @Override
        public void onResult(Void result) {
            Log.e(LOG_TAG, "Sign out succeeded");
        }

        @Override
        public void onError(Exception e) {
            Log.e(LOG_TAG, "Sign out failed");
        }
    });
```
{% include tab_content_end.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
    authUI.signOut(object : Callback<Void>() {
        fun onResult(result: Void) {
            Log.e(LOG_TAG, "Sign out succeeded")
        }

        fun onError(e: Exception) {
            Log.e(LOG_TAG, "Sign out failed")
        }
    })
```

{% include tab_content_end.html %}

## Change Password

{% include tabs.html %}

{% include tab_content_start.html lang="java" %}

```java
authUI.changePassword(activity, new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                MessageDialog.showDialog(activity, "Password Change", "Password change successfully.");
            }

            @Override
            public void onError(Exception e) {
                MessageDialog.showDialog(activity, "Password Change", "Password change failed.");
            }
        });
```

{% include tab_content_end.html %}

{% include tab_content_start.html lang="kt" %}

```kotlin
authUI.changePassword(activity, object : Callback<Void>() {
    fun onResult(result: Void) {
        MessageDialog.showDialog(activity, "Password Change", "Password change successfully.")
    }

    fun onError(e: Exception) {
        MessageDialog.showDialog(activity, "Password Change", "Password change failed.")
    }
})
```

{% include tab_content_end.html %}

## Configuration options

Configurable attributes:
1. Background logo : put custom logo in the res/drawable of the app module with name amplify_authui_logo.png 
2. Background Color : create a string resource with name "backgroundColor" in app module
3. Font Family : create a string resource with name "fontFamily" in app module
4. Screen Texts : For instance, to change the text displayed in the username text box of the sign-in screen
override "R.string.amplify_authui_sign_in_username" string resource in your strings.xml. For an exhaustive list of all the the attributes you can override visit [strings.xml](https://github.com/awslabs/aws-amplify-android/blob/auth-ui-changes/aws-amplify-authui/src/main/res/values/strings.xml) 
5. Button Attributes : You can modify the button background drawable by overriding library's button_background_drawable.xml
6. Screen Background : You can modify the screen bakground by overriding background_drawable.xml inside your drawable folder. 


## API Reference

[Show me the APIs](/reference)

{% include footer.html %}
