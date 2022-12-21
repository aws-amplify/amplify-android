# Contributing Guidelines

Thank you for your interest in contributing to the Android distribution
of the Amplify Framework. Whether it's a bug report, new feature,
correction, or additional documentation, the project maintainers at AWS
greatly value your feedback and contributions.

Please read through this document before submitting any issues or pull
requests. Doing so will help to ensure that the project maintainers have
all information necessary to effectively respond to your bug report or
contribution.

- [Contributing Guidelines](#contributing-guidelines)
  - [Getting Started](#getting-started)
    - [Consuming Development Versions of the Framework](#consuming-development-versions-of-the-framework)
  - [Tools](#tools)
  - [Workflows](#workflows)
    - [Adding Code to Support a New Feature](#adding-code-to-support-a-new-feature)
    - [Build and Validate Your Work](#build-and-validate-your-work)
    - [Run Instrumentation Tests](#run-instrumentation-tests)
    - [Test changes to CodeBuild build definitions](#test-changes-to-codebuild-build-definitions)
  - [Reporting Bugs/Feature Requests](#reporting-bugsfeature-requests)
  - [Contributing via Pull Requests](#contributing-via-pull-requests)
  - [Troubleshooting](#troubleshooting)
    - [Environment Debugging](#environment-debugging)
    - [Problems with the Build](#problems-with-the-build)
    - [Getting More Output](#getting-more-output)
    - [Failing Instrumentation Tests](#failing-instrumentation-tests)
  - [Related Repositories](#related-repositories)
  - [Finding Contributions to Make](#finding-contributions-to-make)
  - [Code of Conduct](#code-of-conduct)
  - [Security Issue Notifications](#security-issue-notifications)
  - [Licensing](#licensing)

## Getting Started

First, ensure that you have installed the latest stable version of Android
Studio / the Android SDK.

Configure your environment, so that the `ANDROID_HOME` and `JAVA_HOME`
environment variables are set. A convenient way of doing this is to add them
into `~/.bashrc`. On a Mac, the SDK and Java installation used by the SDK may
be found:

```shell
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jre/jdk/Contents/Home
```
Note: JDK 11, 12, 13, etc. have known issues and are not supported.

Now, clone the Amplify Android project from GitHub.

```shell
git clone git@github.com:aws-amplify/amplify-android.git
```
Load this project into Android Studio by selecting File > Open, and choosing
the root directory of the project (`amplify-android`). Alternately, cd into this
top-level directory.

In Android Studio, build the project by clicking the Hammer icon, "Make
Project ⌘F9". If working on the command line, you can do the same thing
via:

```shell
./gradlew build
```

### Consuming Development Versions of the Framework

Once you've built the framework, you can manually install the Framework
by publishing its artifacts to your local Maven repository.

The local Maven repository is usually found in your home directory at
`~/.m2/repository`.

To publish the outputs of the build, execute the following command from
the root of the `amplify-android` project:

```shell
./gradlew publishToMavenLocal
```

After this, you can use the published development artifacts from an app.
To do so, specify `mavenLocal()` inside the app's top-level
`build.gradle(Project)` file:

```gradle
buildscript {
    repositories {
        mavenLocal() // this should ideally appear before other repositories
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.0.1'
    }
}

allprojects {
    repositories {
        mavenLocal() // this should ideally appear before other repositories
    }
}
```
Then, find the `VERSION_NAME` of the *library* inside `gradle.properties` file.

Use the above version to specify dependencies in your *app*'s `build.gradle (:app)` file:
```
dependencies {
    implementation 'com.amplifyframework:core:VERSION_NAME'
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

First, identify the module you'll modify. The various Gradle modules are
described below:

 - `core` - The Framework itself, including category behavior definitions
 - `aws-auth-cognito` - A plugin implementation of the Auth category that
    speaks to Amazon Cognito through the legacy `AWSMobileClient` utility
 - `aws-datastore` - An AppSync-based plugin for the DataStore category
 - `aws-api` - Plugin for API category with special abilities to talk to
    AppSync and API Gateway endpoints
 - `aws-api-appsync` - AppSync implementation details that are shared
    accross multiple plugins implementations (`aws-api`, `aws-datastore`)
 - `aws-storage-s3` - A plugin for the Storage category, leveraging S3, Cognito
 - `aws-predictions` - A plugin for Predictions category, leveraging
    numerous Amazon machine learnings services.
 - `aws-predictions-tensorflow` - A plugin for the Predictions category
    which does machine learning on the device, using TensorFlow Lite
 - `aws-analytics-pinpoint` - A plugin for the Analytics category,
    leveraging Amazon Pinpoint
 - `rxbindings` - A front-end for Amplify that expresses operations as
    Rx primitives
 - `amplify-tools` - A Gradle plugin that can be used to run Amplify CLI
    commands from within the Android Studio IDE.
 - `testutils` - Utility code, helpful when writing unit and instrumentation
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

```console
aws-datastore/src/main/com/amplifyframework/datastore/syncengine/Merger.java
aws-datastore/src/test/com/amplifyframework/datastore/syncengine/MergerTest.java
```

The code below might be a reasonable template for these new files.

```java
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


```java
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

```shell
./gradlew clean build
```

Tip: Checkstyle specifies a specific ordering and spacing of import statements, maximum line length, and other rules.
To setup the Android Studio editor to automatically organize import according to the project checkstyle,  go to
Preferences > Editor > Code Style > Java.  Under Scheme, select "Project".

### Run Instrumentation Tests

The instrumentation tests presume the presence of various backend resources.
Currently, there is no mechanism for contributors to easily allocate
these resources. This is tracked in [Amplify issue 301](https://github.com/aws-amplify/amplify-android/issues/301).

AWS maintainers can gain access to `awsconfiguration.json` and
`amplifyconfiguration.json` files in an S3 bucket, to find
configurations suitable for running the integration tests.

If you are part of the private access list, the command below will copy
those configurations to your local workspace:

```shell
cd amplify-android
.circleci/copy-configs
```

To run a __specific__ test:

```shell
test='com.amplifyframework.api.aws.RestApiInstrumentationTest#getRequestWithIAM'
./gradlew cAT -Pandroid.testInstrumentationRunnerArguments.class="$test"
```

To run __all__ tests:

```shell
./gradlew cAT
```

### Test changes to CodeBuild build definitions

Changes made to one of the buildspec files under the `./scripts` folder can be tested locally inside a docker container (See [setup instructions](https://docs.aws.amazon.com/codebuild/latest/userguide/use-codebuild-agent.html) in the CodeBuild docs).

The following command will spin up a docker container locally and run through the build definition in the buildspec file.

```bash
./scripts/codebuild_build.sh -i aws/codebuild/standard:4.0  -a build/codebuild-out -s . -d -m -c -b "scripts/<build_spec_file>.yml"
```

Note that the `-c` option pulls in AWS configuration from your local environment into the docker container. That means any `AWS_*` environment variables will be set inside the container. This is useful when the build process needs to access AWS resources from an AWS account. **Be sure to check which AWS account your current credentials belong to and which permissions are granted**. Typically, the target account should be a development account.

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

```shell
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

```console
[aws-datatore] Add a 3-way merging component for network ingress

The Merger checks the state of the Mutation Outbox before applying
remote changes, locally. Subscriptions, Mutation responses, and
base/delta sync all enter the local storage through the Merger.

Resolves: https://github.com/amplify-android/issues/222
See also: https://stackoverflow.com/a/58662077/695787
```

Now, save your work to a new branch:

```shell
git checkout -B add_merger_to_datastore
```

To publish it:

```shell
git push -u origin add_merger_to_datastore
```

This last step will give you a URL to view a GitHub page in your browser.
Copy-paste this, and complete the workflow in the UI. It will invite you to
"create a PR" from your newly published branch.

### Pull Request Guidelines
- The title of your PR must be descriptive to the specific change.
- The title of your PR must be of below format since next release version is determined from PR titles in the commit history.
    - For a bugfix: `fix(category): description of changes`
    - For a feature: `feat(catgory): add awesome feature`
    - For a release: `release: release version`
    - Everything else: `chore: fix build script`
    - Valid categories are:
      - all
      - analytics
      - api
      - auth
      - core
      - datastore
      - geo
      - predictions
      - storage
    - Eg. `fix(auth): throw correct auth exception for code mismatch`. Refer https://github.com/aws-amplify/amplify-android/pull/1370
- No period at the end of the title.
- Pull Request message should indicate which issues are fixed: `fixes #<issue>` or `closes #<issue>`.
- If not obvious (i.e. from unit tests), describe how you verified that your change works.
- If this PR includes breaking changes, they must be listed at the top of the changelog as described above in the Pull Request Checklist.
- PR must be reviewed by at least one repository maintainer, in order
to be considered for inclusion.
- PR must also pass the CodeBuild workflow and LGTM validations. CodeBuild
will run all build tasks (Checkstyle, Lint, unit tests).
- Usually all these are going to be **squashed** when you merge to main.
- Make sure to update the PR title/description if things change.
- Rebase with the `main` branch if it has commits ahead of your fork.

## Troubleshooting

### Environment Debugging

Are you using the right versions of Gradle, Ant, Groovy, Kotlin, Java, Mac OS X?
```console
./gradlew -version

------------------------------------------------------------
Gradle 6.6
------------------------------------------------------------

Build time:   2020-08-10 22:06:19 UTC
Revision:     d119144684a0c301aea027b79857815659e431b9

Kotlin:       1.3.72
Groovy:       2.5.12
Ant:          Apache Ant(TM) version 1.10.8 compiled on May 10 2020
JVM:          1.8.0_242-release (JetBrains s.r.o 25.242-b3-6222593)
OS:           Mac OS X 10.15.6 x86_64
```

Do you have the Android SDK setup, and do you have a pointer to the Java environment?

```console
echo -e $ANDROID_HOME\\n$JAVA_HOME 
/Users/jhwill/Library/Android/sdk
/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home
```

### Problems with the Build

If the build fails, and you can't figure out why from a Google search /
StackOverflow session, try passing options to Gradle:

```shell
./gradlew --stacktrace
```

The next flag will spit out lots of info. It's only useful if you pipe the
output to a file, and grep through it.

```shell
./gradlew --debug 2>&1 > debugging-the-build.log
```

### Getting More Output

The Amplify Android library emits logs while it is running on a device
or emulator. By default, debug and verbose logs are not output.
However, you can change the log threshold at runtime, by explicitly
configuring a logging plugin:
```kotlin
Amplify.addPlugin(AndroidLoggingPlugin(LogLevel.VERBOSE))
// ... Add more plugins only *after* setting the log plugin.
```

### Failing Instrumentation Tests

If a single test is failing, run only that test, to isolate it. You may also
want to see the output on the device that occurs, while the tests are
executing.

```shell
# If you run this same code in a script/block, this will clear the log
# file each time.
rm -f device-logs-during-test.log

# Clear the device's, so you only get recent stuff.
adb logcat -c

# Run a particular test.
test='com.amplifyframework.api.aws.RestApiInstrumentationTest#getRequestWithIAM'
./gradlew cAT -Pandroid.testInstrumentationRunnerArguments.class="$test"

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

1. [AWS Amplify for Flutter](https://github.com/aws-amplify/amplify-flutter)
2. [AWS Amplify for iOS](https://github.com/aws-amplify/amplify-ios)
3. [AWS Amplify for JavaScript](https://github.com/aws-amplify/amplify-js)
4. [AWS Amplify CLI](https://github.com/aws-amplify/amplify-cli)

AWS Amplify plugins are built on top of "low-level" AWS SDKs. AWS SDKs are a
toolkit for interacting with AWS backend resources.

1. [AWS SDK for Android](https://github.com/aws-amplify/aws-sdk-android)
2. [AWS SDK for iOS](https://github.com/aws-amplify/aws-sdk-ios)
3. [AWS SDK for JavaScript](https://github.com/aws/aws-sdk-js-v3)

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
