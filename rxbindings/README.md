# amplify-android-rxbindings
A wrapper for the AWS Amplify Android framework, which exposes
functionalities via RxJava2

Instead of using Amplify's native callback interface, you can interact
with Amplify APIs by means of RxJava2
[`Observable`](http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html),
[`Single`](http://reactivex.io/RxJava/javadoc/io/reactivex/Single.html), and
[`Completable`](http://reactivex.io/RxJava/javadoc/io/reactivex/Completable.html).

## Usage

### Pre-requisites

As a pre-requisite, deploy AWS resources to your backend using the
Amplify CLI, as in the [Getting
Started guide](https://aws-amplify.github.io/docs/android/start).


### Gradle
Take an `implementation` dependency on this library. In your module's
`build.gradle`:
```
dependencies {
    # ...
    implementation 'com.amplifyframework:core:0.9.1'
    implementation 'com.amplifyframework:rxbindings:0.9.1'
    # ...
}
```

You almost certainly want to enable Java 8 features, so that you can use
lambdas with Rx. In your module's `build.gradle`:

```
android {
    # ...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    # ...
}
```

### Initialization
Initialize Amplify. For convenience, the `RxAmplify` facade exposes the
same functionalities as the core `Amplify` facade:

```
RxAmplify.addPlugin(new AWSAPIPlugin());
RxAmplify.configure(getApplicationContext());
```

### Basic Example
Lastly, use Rx-idiomatic expressions of the Amplify categories. For
example, this will query a GraphQL API for models of type `Person`. If
the query returns a successful response with data, each one will be
printed out.

```
RxAmplify.API.query(Person.class)
    .map(response -> response.data())
    .flatMapObservable(results -> Observable.fromIterable(results))
    .subscribe(person -> {
        Log.i(TAG, "Found a person named " + person.getFirstName());
    });
```

