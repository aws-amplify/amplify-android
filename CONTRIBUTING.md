# Contributing Guidelines

Thank you for your interest in contributing to the Android distribution
of the Amplify Framework. Whether it's a bug report, new feature,
correction, or additional documentation, the project maintainers at AWS
greatly value your feedback and contributions.

Please read through this document before submitting any issues or pull
requests. Doing so will help to ensure that the project maintainers have
all information neessary to effectively respond to your bug report or
contribution.

- [Contributing Guidelines](#contributing-guidelines)
  * [Getting Started](#getting-started)
    + [Consuming Development Versions of the Framework](#consuming-development-versions-of-the-framework)
  * [Tools](#tools)
  * [Workflows](#workflows)
    + [Adding Code to Support a New Feature](#adding-code-to-support-a-new-feature)
    + [Build and Validate Your Work](#build-and-validate-your-work)
    + [Run Instrumentation Tests](#run-instrumentation-tests)
  * [Reporting Bugs/Feature Requests](#reporting-bugs-feature-requests)
  * [Contributing via Pull Requests](#contributing-via-pull-requests)
  * [Troubleshooting](#troubleshooting)
    + [Environment Debugging](#environment-debugging)
    + [Problems with the Build](#problems-with-the-build)
    + [Failing Instrumentation Tests](#failing-instrumentation-tests)
  * [Related Repositories](#related-repositories)
  * [Finding Contributions to Make](#finding-contributions-to-make)
  * [Code of Conduct](#code-of-conduct)
  * [Security Issue Notifications](#security-issue-notifications)
  * [Licensing](#licensing)

## Getting Started

First, ensure that you have installed the latest stable version of Android
Studio / the Android SDK.

Configure your environment, so that the `ANDROID_HOME` and `JAVA_HOME`
environment variables are set. A convenient way of doing this is to add them
into `~/.bashrc`. On a Mac, the SDK and Java installation used by the SDK may
be found:

```
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jre/jdk/Contents/Home
```
Note: JDK 11, 12, 13, etc. have known issues and are not supported.

Now, clone the Amplify Android project from GitHub.

```
git clone git@github.com:aws-amplify/amplify-android.git
```
Load this project into Android Studio by selecting File > Open, and choosing
the root directory of the project (`amplify-android`). Alternately, cd into this
top-level directory.

In Android Studio, build the project by clicking the Hammer icon, "Make
Project ⌘F9". If working on the command line, you can do the same thing
via:

```
./gradlew build
```

### Consuming Development Versions of the Framework

Once you've built the framework, you can manually install the Framework
by publishing its artifacts to your local Maven repository.

The local Maven repository is usually found in your home directory at
`~/.m2/repository`.

To publish the outputs of the build, execute the following command from
the root of the `amplify-android` project:

```
./gradlew publishToMavenLocal
```

After this, you can use the published development artifacts from an app.
To do so, specify `mavenLocal()` inside the app's top-level
`build.gradle` file:

```gradle
buildscript {
    repositories {
        mavenLocal() // this should ideally appear before other repositories
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.6.3'
    }
}

allprojects {
    repositories {
        mavenLocal() // this should ideally appear before other repositories
    }
}
```

## Tools
[Gradle](https://gradle.org) is used for all [build and dependency management](https://developer.android.com/studio/build).

Some widely used dependencies are:

1. [org.json](https://developer.android.com/reference/org/json/JSONObject) is
   baked into Android, and is used for all modeling with JSON.
2. [Gson](https://github.com/google/gson/blob/master/README.md#gson) is used
   for serialization and deserialization.
3. [OkHttp](https://github.com/square/okhttp#okhttp) is used for network operations.

_Unit and component tests_, which run on your development machine, use:

1. [jUnit](https://github.com/junit-team/junit4/blob/r4.13/README.md#junit-4), to make assertions
2. [Mockito](https://javadoc.io/static/org.mockito/mockito-core/1.10.19/org/mockito/Mockito.html#1), to mock dependencies
3. [Robolectric](https://github.com/robolectric/robolectric/blob/master/README.md),
   to simulate an Android device when unit tests execute on your machine.

_Instrumentation tests_, which run on an Android device or emulator use
AndroidX test core, runner, and a jUnit extension. See Android's notes on
[using AndroidX for test](https://developer.android.com/training/testing/set-up-project).

## Workflows

### Adding Code to Support a New Feature

Be aware of the Getting Started and Pull Request guides. This portion deals
actually with changing some code.

First, identify the module you'll modify:

 - `core` - The Framework itself, including category behavior definitions
 - `aws-datastore` - An AppSync implementation of the datastore contract
 - `aws-api` - A utility to talk to GraphQL and REST endpoints
 - `aws-storage-s3` - Wrapper around S3
 - `aws-analytics-pinpoint` - Wrapper around Pinpoint
 - `testutilts` - Utility code, helpful when writing unit and instrumentation
	tests. A lot of it deals with making async code synchronous, for more
    legible tests.
 - `testmodels` - Models that are used in test code. These were generated by
    the Amplify CLI / code-gen. We have them checked-in to be sure they don't
    change and break some tests.

You should proceed by creating a new Java file, and writing your feature in
isolation. This way, you can write independent unit tests for your feature.
Unit tests are required for all features being added to the code-base. Once you
have the skeleton of your feature working, you can integrate it into the
existing source files.

Writing good Android code and good tests is far outside the scope of this
document. But, to get started, you can use the templates below which show some
conventions and expectations for source files in the Android codebase.

For example, let's say you want to add a `Merger` component to the AppSync
Local implementation. Create a file in that module, and a unit test for it:

```
aws-datastore/src/main/com/amplifyframework/datastore/syncengine/Merger.java
aws-datastore/src/test/com/amplifyframework/datastore/syncengine/MergerTest.java
```

The code below might be a reasonable template for these new files.

```
/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

/**
 * Considers the state of the mutation outbox, before applying
 * data to the local storage adapter. Inputs to the merger
 * are data from the network.
 */
final class Merger {
    private final LocalStorageAdapter localStorageAdapter;
    private final MutationOutbox mutationOutbox;

    Merger(
            @NonNull LocalStorageAdapter localStorageAdapter,
            @NonNull MutationOutbox mutationOutbox) {
        this.localStorageAdapter = localStorageAdapter;
        this.mutationOutbox = mutationOutbox;
    }

    ...
}
```


```
/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.syncengine;

... imports here ...

/**
 * Tests the {@link Merger}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MergerTest {
    private LocalStorageAdapter localStorageAdapter;
    private MutationOutbox mutationOutbox;

    // Setup your object under test in @Before. Try to mock away
    // its dependencies.
    @Before
    public void setup() {
        this.localStorageAdapter = mock(LocalStorageAdpater.class);
        this.mutationOutbox = mock(MutationOutbox.class);
        this.merger = new Merger(localStorageAdapter, mutationOutbox);
    }

    // Name the test in a way that you can tell the developer's intention
    // why did you decide to write this test? What should the code _do_?
    @Test
    public void mergerAppliesUpdates() {
        // Arrange: document preconditions for this test here
        ...

        // Act: what is the action of this test?
        ...
        // Assert: Given the arrangement, how does the code respond to the action?
    }
}
```

These templates encode some standards and best practices.
[Checkstyle](https://github.com/checkstyle/checkstyle/blob/master/README.md) and
[Android Lint](https://developer.android.com/studio/write/lint#overview)
are the ultimate authorities on this, and will tell you if something is
badly formatted, or exciting a known shortcoming in Java, Android, or
their library ecosystem.

Some suggestions include:

* All files get a copyright header with the year of creation;
* Use [`@NonNull`](https://developer.android.com/reference/kotlin/androidx/annotation/NonNull)
  and [`@Nullable`](https://developer.android.com/reference/kotlin/androidx/annotation/Nullable)
  annotations on any/all params, so that
  [Kotlin knows what to do](https://kotlinlang.org/docs/reference/java-interop.html#nullability-annotations),
  when it consumes the library;
* Label Arrange/Act/Assert portions of your unit tests, so others can follow
  their flow;
* Arrange shared dependencies and test objects in an
  [`@Before`](http://junit.sourceforge.net/javadoc/org/junit/Before.html) method;
* Prefer `private` and `final` class members where-ever possible. This limits scope
  and mutability;
* Add documentation to anything that says `public` or `protected`. The
  checkstyle doesn't require documentation on package-local or `private` fields, but
  that might be a good idea, too, if you're doing some interesting/non-trivial
  work in those places.

### Build and Validate Your Work

This will perform a clean build, run Checkstyle, Android Lint, and all unit
tests. This must complete successfully before proposing a PR.

```
./gradlew clean build
```

### Run Instrumentation Tests

The instrumentation tests presume the presence of various backend resources.
Currently, there is no mechanism for contributors to easily allocate
these resources. This is tracked in [Amplify issue 301](https://github.com/aws-amplify/amplify-android/issues/301).

AWS maintainers can gain access to `awsconfiguration.json` and
`amplifyconfiguration.json` files in an S3 bucket, to find
configurations suitable for running the integration tests.

If you are part of the private access list, the command below will copy
those configurations to your local workspace:

```
cd amplify-android
.circleci/copy-configs
```

To run a __specific__ test:

```
test='com.amplifyframework.api.aws.RestApiInstrumentationTest#getRequestWithIAM'
./gradlew :cAT -Pandroid.testInstrumentationRunnerArguments.class="$test"
```

To run __all__ tests:

```
./gradlew cAT
```

## Reporting Bugs/Feature Requests

We welcome you to use the GitHub issue tracker to report bugs or suggest
features.

When filing an issue, please check [existing open](https://github.com/awslabs/amplify-android/issues)
and [recently closed](https://github.com/awslabs/amplify-android/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aclosed%20)
issues to make sure somebody else hasn't already reported the issue.
Please try to include as much information as you can. Details like these
are useful:

* The version of the Framework you are using
* Details and configurations for any backend resources that are relevant
* A full exception trace of an error you observe
* A statement about what system behavior you _expect_, alongside the
  behavior you actually observe

## Contributing via Pull Requests

This is mostly the same as [GitHub's guide on creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).

First, create a _fork_ of `amplify-android`. Clone it, and make changes to this _fork_.

```
git clone git@github.com:your_username/amplify-android.git 
```

After you have tested your feature/fix, by adding sufficient test coverage, and
validating Checkstyle, lint, and the existing test suites, you're ready to
publish your change.

The commit message should look like below. It started with a bracketed tag
stating which module has been the focus of the change. After a paragraph
describing what you've done, include links to useful resources. These might
include design documents, StackOverflow implementation notes, GitHub issues,
etc. All links must be publicly accessible.

```
[aws-datatore] Add a 3-way merging component for network ingress

The Merger checks the state of the Mutation Outbox before applying
remote changes, locally. Subscriptions, Mutation responses, and
base/delta sync all enter the local storage through the Merger.

Resolves: https://github.com/amplify-android/issues/222
See also: https://stackoverflow.com/a/58662077/695787
```

Now, save your work to a new branch:

```
git checkout -B add_merger_to_datastore
```

To publish it:

```
git push origin origin add_merger_to_datastore
```

This last step will give you a URL to view a GitHub page in your browser.
Copy-paste this, and complete the workflow in the UI. It will invite you to
"create a PR" from your newly published branch.

Your should add the
**[Amplify-Native](https:~~/~~/github.com/orgs/aws-amplify/teams/amplify-native)**
team as a reviewer of your PR.

Your PR must be reviewed by at least one member of this team, in order to be
considered for inclusion.

your PR must also pass the CircleCI workflow and LGTM validations. CircleCI
will run all build tasks (Checkstyle, Lint, unit tests).

Currently, CircleCI **DOES NOT** run instrumentation tests for PRs that come
from user forks. You should run these tests on your laptop before submitting
the PR.

## Troubleshooting

### Environment Debugging

Are you using the right versions of Gradle, Ant, Groovy, Kotlin, Java, Mac OS X?
```
./gradlew -version

------------------------------------------------------------
Gradle 6.3
------------------------------------------------------------

Build time:   2020-03-24 19:52:07 UTC
Revision:     bacd40b727b0130eeac8855ae3f9fd9a0b207c60

Kotlin:       1.3.70
Groovy:       2.5.10
Ant:          Apache Ant(TM) version 1.10.7 compiled on September 1 2019
JVM:          1.8.0_212-release (JetBrains s.r.o 25.212-b4-5784211)
OS:           Mac OS X 10.14.6 x86_64
```

Do you have the Android SDK setup, and do you have a pointer to the Java environment?

```
echo -e $ANDROID_HOME\\n$JAVA_HOME 
/Users/jhwill/Library/Android/sdk
/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home
```

### Problems with the Build

If the build fails, and you can't figure out why from a Google search /
StackOverflow session, try passing options to Gradle:

```
./gradlew --stacktrace
```

The next flag will spit out lots of info. It's only useful if you pipe the
output to a file, and grep through it.

```
./gradlew --debug 2>&1 > debugging-the-build.log
```

### Failing Instrumentation Tests

If a single test is failing, run only that test, to isolate it. You may also
want to see the output on the device that occurs, while the tests are
executing.

```
# If you run this same code in a script/block, this will clear the log
# file each time.
rm -f device-logs-during-test.log

# Clear the device's, so you only get recent stuff.
adb logcat -c

# Run a particular test.
test='com.amplifyframework.api.aws.RestApiInstrumentationTest#getRequestWithIAM'
./gradlew :cAT -Pandroid.testInstrumentationRunnerArguments.class="$test"

# Dump the device logs to your debugging file.
adb logcat -d > device-logs-during-test.log
```

Now, you can inspect both the test execution results, as well as what was
happening on the device, in `device-logs-during-test.log`.

## Related Repositories

This project is part of the Amplify Framework, which runs on Android,
iOS, and numerous JavaScript-based web platforms. The Amplify CLI
provides an entry point to configure backend resources for all of these
platforms.

1. [AWS Amplify CLI](https://github.com/aws-amplify/amplify-cli)
2. [AWS Amplify for iOS](https://github.com/aws-amplify/amplify-ios)
3. [AWS Amplify for JavaScript](https://github.com/aws-amplify/amplify-js)

AWS Amplify plugins are built on top of the AWS SDKs. AWS SDKs are a
toolkit for interacting with AWS backend resources.

1. [AWS SDK for Android](https://github.com/aws-amplify/aws-sdk-android)
2. [AWS SDK for iOS](https://github.com/aws-amplify/aws-sdk-ios)
3. [AWS SDK for JavaScript](https://github.com/aws/aws-sdk-js)

Not officially part of the AWS SDKs, [AppSync](https://aws.amazon.com/appsync/) is an opinionated,
mobile-oriented GraphQL management service. It is used by Amplify's
DataStore and API plugins.

1. [Android AppSync Client](https://github.com/awslabs/aws-mobile-appsync-sdk-android)
2. [iOS AppSync Client](https://github.com/awslabs/aws-mobile-appsync-sdk-ios)
3. [JavaScript AppSync Client](https://github.com/awslabs/aws-mobile-appsync-sdk-js)

## Finding Contributions to Make
Looking at [the existing issues](https://github.com/aws-amplify/amplify-android/issues) is a
great way to find something to work on.

## Code of Conduct
This project has adopted the [Amazon Open Source Code of Conduct](https://aws.github.io/code-of-conduct).
For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq) or contact
opensource-codeofconduct@amazon.com with any additional questions or comments.

## Security Issue Notifications
If you discover a potential security issue in this project we ask that you notify AWS/Amazon Security via our
[vulnerability reporting page](http://aws.amazon.com/security/vulnerability-reporting/). Please
do **not** create a public GitHub issue.

## Licensing

See the
[LICENSE](https://github.com/awslabs/amplify-android/blob/master/LICENSE)
for more information. We will ask you to confirm the licensing of your
contribution.

We may ask you to sign a
[Contributor License Agreement (CLA)](http://en.wikipedia.org/wiki/Contributor_License_Agreement) for
larger changes.
