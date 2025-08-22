# Rx Bindings

The Rx Bindings provide a facade to the Amplify library.  Instead of
using Amplify's native callback interface, you can interact with Amplify
APIs by means of RxJava3
[`Observable`](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Observable.html),
[`Single`](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Single.html), and
[`Completable`](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Completable.html).

For more information see [the public documentation](https://docs.amplify.aws/lib/project-setup/rxjava/q/platform/android)
and [this blog post](https://aws.amazon.com/blogs/mobile/using-rxjava-with-aws-amplify-android-library/).

## Usage

### Pre-requisites

All of the pre-requisites in the [Getting
Started guide](https://docs.amplify.aws/start/q/integration/android) apply
as normal.

### Gradle
To start using the Rx APIs, take an `implementation` dependency on this
library. In your module's `build.gradle`:
```gradle
dependencies {
    // Add this line.
    implementation 'com.amplifyframework:rxbindings:2.29.2'
}
```

### Initialization
Initialize Amplify through the `RxAmplify` facade instead of through
`Amplify` directly. The `RxAmplify` facade exposes the same
functionalities as the core `Amplify` facade:

```java
RxAmplify.addPlugin(new AWSAPIPlugin());
RxAmplify.configure(getApplicationContext());
```

### Basic Example
This example will query a GraphQL API for models of type `Person`.  If
the query returns a successful response with data, each one will be
printed out.

```java
RxAmplify.API.query(Person.class)
    .map(response -> response.data())
    .flatMapObservable(results -> Observable.fromIterable(results))
    .subscribe(person -> {
        Log.i(TAG, "Found a person named " + person.getFirstName());
    });
```

