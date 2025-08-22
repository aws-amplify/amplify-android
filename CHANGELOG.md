## [Release 2.29.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.29.2)

### Bug Fixes
- **auth:** Select first non http(s) redirect ([#3091](https://github.com/aws-amplify/amplify-android/issues/3091))
- **analytics:** Remove test dependency ([#3093](https://github.com/aws-amplify/amplify-android/issues/3093))

[See all changes between 2.29.1 and 2.29.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.29.1...release_v2.29.2)

## [Release 2.29.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.29.1)

### Bug Fixes
- **notifications:** Update to latest firebase bom ([#3086](https://github.com/aws-amplify/amplify-android/issues/3086))

[See all changes between 2.29.0 and 2.29.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.29.0...release_v2.29.1)

## [Release 2.29.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.29.0)

### Features
- **predictions:** Add support for a no light liveness challenge ([#3083](https://github.com/aws-amplify/amplify-android/issues/3083))

### Miscellaneous
- Remove ignored tests ([#3079](https://github.com/aws-amplify/amplify-android/issues/3079))

[See all changes between 2.28.0 and 2.29.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.28.0...release_v2.29.0)

## [Release 2.28.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.28.0)

### Bug Fixes
- **auth:** Emit SIGNED_IN event after autoSignIn ([#3068](https://github.com/aws-amplify/amplify-android/issues/3068))
- **storage:** Improve exception handling when attempting to overwrite a file during download ([#3056](https://github.com/aws-amplify/amplify-android/issues/3056))

### Miscellaneous
- fix(data) Stabilize API/Auth tests ([#3065](https://github.com/aws-amplify/amplify-android/issues/3065))

[See all changes between 2.27.4 and 2.28.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.27.4...release_v2.28.0)

## [Release 2.27.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.27.4)

### Bug Fixes
- **auth:** Fix passkey usage on Android < 13 devices ([#3039](https://github.com/aws-amplify/amplify-android/issues/3039))
- **auth:** Update exception handling for devices that don't support passkeys ([#3044](https://github.com/aws-amplify/amplify-android/issues/3044))
- **auth:** Remove WindowManager usage from DeviceDataCollector ([#3061](https://github.com/aws-amplify/amplify-android/issues/3061))

[See all changes between 2.27.3 and 2.27.4](https://github.com/aws-amplify/amplify-android/compare/release_v2.27.3...release_v2.27.4)

## [Release 2.27.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.27.3)

### Bug Fixes
- **storage:** Fix rare edge case of transfer status update crash ([#3006](https://github.com/aws-amplify/amplify-android/issues/3006))
- **logging:** Fixes issue of logging category not initializing properly using gen2 config ([#3022](https://github.com/aws-amplify/amplify-android/issues/3022))

[See all changes between 2.27.2 and 2.27.3](https://github.com/aws-amplify/amplify-android/compare/release_v2.27.2...release_v2.27.3)

## [Release 2.27.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.27.2)

### Bug Fixes
- **all:** Add additional consumer proguard rules for missing classes ([#3000](https://github.com/aws-amplify/amplify-android/issues/3000))

[See all changes between 2.27.1 and 2.27.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.27.1...release_v2.27.2)

## [Release 2.27.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.27.1)

### Bug Fixes
- **auth:** Prevent crash if KeyStore not available (ex: instant app) ([#2993](https://github.com/aws-amplify/amplify-android/issues/2993))
- **storage:** Prevent duplicate observer registration in TransferWorkerObserver to mitigate OOM issues ([#2988](https://github.com/aws-amplify/amplify-android/issues/2988))

[See all changes between 2.27.0 and 2.27.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.27.0...release_v2.27.1)

## [Release 2.27.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.27.0)

### Bug Fixes
- **auth:** Return cached user id and username instead of refetching them in getCurrentUser ([#2985](https://github.com/aws-amplify/amplify-android/issues/2985))

[See all changes between 2.26.0 and 2.27.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.26.0...release_v2.27.0)

## [Release 2.26.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.26.0)

### Features
- **all:** Add ability to use OkHttp4 with Amplify v2.x ([#2970](https://github.com/aws-amplify/amplify-android/issues/2970))

### Bug Fixes
- **datastore:** base sync when sync expression changes ([#2937](https://github.com/aws-amplify/amplify-android/issues/2937))
- **auth:** Fallback to in-memory key value storage if encryption fails ([#2969](https://github.com/aws-amplify/amplify-android/issues/2969))
- **logging:** CloudWatch Plugin 16KB page size support ([#2919](https://github.com/aws-amplify/amplify-android/issues/2919))
- **storage:** Fix SocketTimeoutException when executing a long multi-part upload ([#2973](https://github.com/aws-amplify/amplify-android/issues/2973))

[See all changes between 2.25.1 and 2.26.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.25.1...release_v2.26.0)

## [Release 2.25.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.25.1)

### Bug Fixes
- **auth:** Fix Device Metadata migration if alised userId was used ([#2963](https://github.com/aws-amplify/amplify-android/issues/2963))

[See all changes between 2.25.0 and 2.25.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.25.0...release_v2.25.1)

## [Release 2.25.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.25.0)

### Features
- **auth:** Add Passwordless features to Amplify ([#2952](https://github.com/aws-amplify/amplify-android/issues/2952))

[See all changes between 2.24.1 and 2.25.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.24.1...release_v2.25.0)

## [Release 2.24.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.24.1)

### Bug Fixes
- **analytics:** Fix accessing the Analytics category from RxAmplify or Kotlin Facade ([#2944](https://github.com/aws-amplify/amplify-android/issues/2944))
- **api:** Fix connecting to AppSync from China with API category ([#2948](https://github.com/aws-amplify/amplify-android/issues/2948))

[See all changes between 2.24.0 and 2.24.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.24.0...release_v2.24.1)

## [Release 2.24.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.24.0)

### Features
- **auth:** Add support for Email MFA ([#2935](https://github.com/aws-amplify/amplify-android/issues/2935))

[See all changes between 2.23.0 and 2.24.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.23.0...release_v2.24.0)

## [Release 2.23.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.23.0)

### Features
- **predictions:** Added region handling for creating correct streaming endpoint from region ([#2923](https://github.com/aws-amplify/amplify-android/issues/2923))
- **api:** Pass authorization in header instead of query parameter for API category ([#2918](https://github.com/aws-amplify/amplify-android/issues/2918))

[See all changes between 2.22.0 and 2.23.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.22.0...release_v2.23.0)

## [Release 2.22.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.22.0)

### Features
- **storage:** implement multiple buckets support ([#2904](https://github.com/aws-amplify/amplify-android/issues/2904))

[See all changes between 2.21.1 and 2.22.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.21.1...release_v2.22.0)

## [Release 2.21.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.21.1)

### Bug Fixes
- **notifications:** Catch Exception when download icon image of pinpoint notification ([#2873](https://github.com/aws-amplify/amplify-android/issues/2873))

[See all changes between 2.21.0 and 2.21.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.21.0...release_v2.21.1)

## [Release 2.21.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.21.0)

### Features
- **storage:** add delimiter support ([#2871](https://github.com/aws-amplify/amplify-android/issues/2871))

### Bug Fixes
- **storage:** Remove unused foreground service permission ([#2877](https://github.com/aws-amplify/amplify-android/issues/2877))

[See all changes between 2.20.0 and 2.21.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.20.0...release_v2.21.0)

## [Release 2.20.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.20.0)

### Features
- **storage:** add object existence validation option to get presigned url ([#2848](https://github.com/aws-amplify/amplify-android/issues/2848))

### Bug Fixes
- **notifications:** Prevent notifications text from being truncated ([#2856](https://github.com/aws-amplify/amplify-android/issues/2856))
- **api:** Don't prevent subscribing with API_KEY when there is also an owner-based rule ([#2828](https://github.com/aws-amplify/amplify-android/issues/2828))

### Miscellaneous
- **analytics:** Update junit ktx dependency ([#2859](https://github.com/aws-amplify/amplify-android/issues/2859))
- **auth:** Fix tests by using proper birthdate attribute according to Cognito ([#2865](https://github.com/aws-amplify/amplify-android/issues/2865))
- **all:** Remove ignored tests ([#2867](https://github.com/aws-amplify/amplify-android/issues/2867))

[See all changes between 2.19.1 and 2.20.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.19.1...release_v2.20.0)

## [Release 2.19.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.19.1)

### Bug Fixes
- **auth:** Fix isSignedIn states ([#2830](https://github.com/aws-amplify/amplify-android/issues/2830))
- **DataStore:** should not crash on missing version metadata ([#2849](https://github.com/aws-amplify/amplify-android/issues/2849))

[See all changes between 2.19.0 and 2.19.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.19.0...release_v2.19.1)

## [Release 2.19.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.19.0)

### Miscellaneous
- DataStore Model Sync Parallelization ([#2808](https://github.com/aws-amplify/amplify-android/issues/2808))

[See all changes between 2.18.0 and 2.19.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.18.0...release_v2.19.0)

## [Release 2.18.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.18.0)

### Features
- **all:** Update to Kotlin SDK 1.2.8 (includes OkHttp Update) ([#2813](https://github.com/aws-amplify/amplify-android/issues/2813))

### Bug Fixes
- **auth:** Fix HostedUI signout cancellation issue ([#2834](https://github.com/aws-amplify/amplify-android/issues/2834))
- **auth:** Fix parsing of REQUIRES_UPPERCASE/REQUIRES_LOWERCASE password settings in Gen1 ([#2836](https://github.com/aws-amplify/amplify-android/issues/2836))

[See all changes between 2.17.0 and 2.18.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.17.0...release_v2.18.0)

## [Release 2.17.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.17.0)

### Features
- **core:** Add APIs to lazily evaluate log statements ([#2811](https://github.com/aws-amplify/amplify-android/issues/2811))

### Bug Fixes
- **analytics:** Make Analytics Options Builder build function public for Java to call ([#2804](https://github.com/aws-amplify/amplify-android/issues/2804))
- **predictions:** Ignore unknown events from liveness websocket ([#2736](https://github.com/aws-amplify/amplify-android/issues/2736))
- **api:** Fix the handling of exceptions when subscribing with Kotlin Facade ([#2821](https://github.com/aws-amplify/amplify-android/issues/2821))
- **auth:** Prevent Rare Deadlock Scenario ([#2822](https://github.com/aws-amplify/amplify-android/issues/2822))
- **api:** Add missing JvmStatic annotations ([#2823](https://github.com/aws-amplify/amplify-android/issues/2823))

[See all changes between 2.16.1 and 2.17.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.16.1...release_v2.17.0)

## [Release 2.16.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.16.1)

### Bug Fixes
- **logging:** Fix unexpected behavior of bulkDelete action ([#2772](https://github.com/aws-amplify/amplify-android/issues/2772)) ([#2776](https://github.com/aws-amplify/amplify-android/issues/2776))
- **auth:** Fix timestamp issue with v1 to v2 migration ([#2799](https://github.com/aws-amplify/amplify-android/issues/2799))
- **datastore:** Reconcile batch process optimization bug [#2727](https://github.com/aws-amplify/amplify-android/issues/2727) ([#2749](https://github.com/aws-amplify/amplify-android/issues/2749))
- **Storage:** Prevent InputStream errors from calling back more than once ([#2742](https://github.com/aws-amplify/amplify-android/issues/2742))
- **datastore:** Prevent datastore error with single auth rule when Mu… ([#2760](https://github.com/aws-amplify/amplify-android/issues/2760))
- **datastore:** List errors that are returned from AppSync ([#2801](https://github.com/aws-amplify/amplify-android/issues/2801))

[See all changes between 2.16.0 and 2.16.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.16.0...release_v2.16.1)

## [Release 2.16.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.16.0)

### Features
- **storage:** Gen2 Storage API ([#2758](https://github.com/aws-amplify/amplify-android/issues/2758))
- **all:** Add Gen2 Config ([#2771](https://github.com/aws-amplify/amplify-android/issues/2771))

[See all changes between 2.15.2 and 2.16.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.15.2...release_v2.16.0)

## [Release 2.15.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.15.2)

### Bug Fixes
- **auth:** Fix incorrect statement about when userId is returned in AuthSignUpResult ([#2782](https://github.com/aws-amplify/amplify-android/issues/2782))
- **storage:** Fix multiple instances of TransferDB leading to SQLiteDatabaseLockedException ([#2786](https://github.com/aws-amplify/amplify-android/issues/2786))

[See all changes between 2.15.1 and 2.15.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.15.1...release_v2.15.2)

## [Release 2.15.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.15.1)

### Bug Fixes
- **auth:** Fix issue where unauthenticated logins caused 401 because refresh caused new credentials to be created ([#2752](https://github.com/aws-amplify/amplify-android/issues/2752))

[See all changes between 2.15.0 and 2.15.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.15.0...release_v2.15.1)

## [Release 2.15.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.15.0)

### Features
- **auth:** Expose AuthConfiguration directly ([#2740](https://github.com/aws-amplify/amplify-android/issues/2740))

### Miscellaneous
- fix(auth) Add underlying cause to fetchAuthSession error ([#2750](https://github.com/aws-amplify/amplify-android/issues/2750))

[See all changes between 2.14.13 and 2.15.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.13...release_v2.15.0)

## [Release 2.14.13](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.13)

### Bug Fixes
- **analytics:** Set start time before generating session ID ([#2747](https://github.com/aws-amplify/amplify-android/issues/2747))

[See all changes between 2.14.12 and 2.14.13](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.12...release_v2.14.13)

## [Release 2.14.12](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.12)

### Bug Fixes
- **Predictions:** Fix Liveness InvalidSignatureException ([#2729](https://github.com/aws-amplify/amplify-android/issues/2729))

[See all changes between 2.14.11 and 2.14.12](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.11...release_v2.14.12)

## [Release 2.14.11](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.11)

### Bug Fixes
- **storage:** upload/download fewer files in storage stress test to avoid S3 throttling exception ([#2712](https://github.com/aws-amplify/amplify-android/issues/2712))
- **datastore:** Remove unnecessary synchronized causing subscription slowness ([#2718](https://github.com/aws-amplify/amplify-android/issues/2718), [#2719](https://github.com/aws-amplify/amplify-android/issues/2719))
- **datastore:** Switch to BFS in Join Builder for Alias, fixes - [#2488](https://github.com/aws-amplify/amplify-android/issues/2488) ([#2693](https://github.com/aws-amplify/amplify-android/issues/2693))

[See all changes between 2.14.10 and 2.14.11](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.10...release_v2.14.11)

## [Release 2.14.10](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.10)

### Features
- **datastore:** Improve Sync Performance Through SQLite Transaction Batching ([#2703](https://github.com/aws-amplify/amplify-android/issues/2703))

### Bug Fixes
- **api:** Use provided Content-Type to create RequestBody ([#2666](https://github.com/aws-amplify/amplify-android/issues/2666))
- **auth:** Add MFAType.challengeResponse extension ([#2680](https://github.com/aws-amplify/amplify-android/issues/2680))
- **core:** use returns instead of answers on coEvery ([#2653](https://github.com/aws-amplify/amplify-android/issues/2653))
- **datastore:** Fix DataStore peek exception ([#2702](https://github.com/aws-amplify/amplify-android/issues/2702))
- **geo:** Fix Geo Plugin Auth injection ([#2704](https://github.com/aws-amplify/amplify-android/issues/2704))
- **all:** Add fallback logic for corrupt keys to EncryptedKeyValueRepository ([#2686](https://github.com/aws-amplify/amplify-android/issues/2686))
- **datastore:** Fix test failure introduced with slower test runs from SQLite ([#2708](https://github.com/aws-amplify/amplify-android/issues/2708))

### Miscellaneous
- Update to AWS Kotlin SDK v1.x stable ([#2695](https://github.com/aws-amplify/amplify-android/pull/2695))

[See all changes between 2.14.9 and 2.14.10](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.9...release_v2.14.10)

## [Release 2.14.9](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.9)

### Bug Fixes
- **storage:** Remove work manager restricted API usage and upgrade lib. ([#2674](https://github.com/aws-amplify/amplify-android/issues/2674))

### Miscellaneous
- Fix CHANGELOG.md for v2.14.8 ([#2664](https://github.com/aws-amplify/amplify-android/issues/2664))

[See all changes between 2.14.8 and 2.14.9](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.8...release_v2.14.9)

## [Release 2.14.8](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.8)

### Miscellaneous
- **liveness:** add close codes to websocket apis ([#2658](https://github.com/aws-amplify/amplify-android/issues/2658))

[See all changes between 2.14.7 and 2.14.8](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.7...release_v2.14.8)


## [Release 2.14.7](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.7)

### Bug Fixes
- **datastore:** Update Model Class Check in getMutationForModelId() from using ModelSchema class name to mutatedItem modelName ([#2637](https://github.com/aws-amplify/amplify-android/issues/2637))

[See all changes between 2.14.6 and 2.14.7](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.6...release_v2.14.7)

## [Release 2.14.6](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.6)

### Bug Fixes
- **auth:** Add userAttributes to confirmSignIn call ([#2640](https://github.com/aws-amplify/amplify-android/issues/2640))

[See all changes between 2.14.5 and 2.14.6](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.5...release_v2.14.6)

## [Release 2.14.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.5)

### Bug Fixes
- **liveness:** correct websocket retry logic ([#2634](https://github.com/aws-amplify/amplify-android/issues/2634))

[See all changes between 2.14.4 and 2.14.5](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.4...release_v2.14.5)

## [Release 2.14.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.4)

### Bug Fixes
- Re-release of 2.14.2. Do not use 2.14.3 due to regression in aws-predictions for Liveness

[See all changes between 2.14.2 and 2.14.4](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.2...release_v2.14.4)

## [Release 2.14.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.2)

### Bug Fixes
- **auth:** Fix for when alias is used to sign in and deviceKey is stored with the entered username instead of retrieved username ([#2614](https://github.com/aws-amplify/amplify-android/issues/2614))
- **datastore:** Add Model Class Check in getMutationById() to Prevent Cross-Model Primary Key Collisions ([#2612](https://github.com/aws-amplify/amplify-android/issues/2612))
- **datastore:** Temporary workaround to stabilize DataStore test. ([#2618](https://github.com/aws-amplify/amplify-android/issues/2618))

[See all changes between 2.14.1 and 2.14.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.1...release_v2.14.2)

## [Release 2.14.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.1)

### Bug Fixes
- **auth:** Fix for retry sign in when resourceNotFoundException is raised ([#2605](https://github.com/aws-amplify/amplify-android/issues/2605))

[See all changes between 2.14.0 and 2.14.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.14.0...release_v2.14.1)

## [Release 2.14.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.14.0)

### Features
- **api:** Lazy Loading and Custom Selection Set ([#2592](https://github.com/aws-amplify/amplify-android/issues/2592))

### Bug Fixes
- **push:** Add previously removed tests ([#2589](https://github.com/aws-amplify/amplify-android/issues/2589))
- **auth:** fetch device metadata from credential store for `remember` and `forgetDevice` ([#2601](https://github.com/aws-amplify/amplify-android/issues/2601))

[See all changes between 2.13.2 and 2.14.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.13.2...release_v2.14.0)

## [Release 2.13.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.13.2)

### Bug Fixes
- **push:** Prevent Unintended OptOuts ([#2587](https://github.com/aws-amplify/amplify-android/issues/2587))

[See all changes between 2.13.1 and 2.13.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.13.1...release_v2.13.2)

## [Release 2.13.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.13.1)

### Bug Fixes
- **liveness:** Read facematch timeout from server event JSON ([#2577](https://github.com/aws-amplify/amplify-android/issues/2577))
- **liveness:** API to add liveness version to websocket ([#2572](https://github.com/aws-amplify/amplify-android/issues/2572))

[See all changes between 2.13.0 and 2.13.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.13.0...release_v2.13.1)

## [Release 2.13.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.13.0)

### Features
- **Auth:** Add TOTP Support ([#2537](https://github.com/aws-amplify/amplify-android/issues/2537)) ([#2568](https://github.com/aws-amplify/amplify-android/issues/2568))

### Miscellaneous
- Update readme & contributing guide to add versioning ([#2544](https://github.com/aws-amplify/amplify-android/issues/2544))

[See all changes between 2.12.0 and 2.13.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.12.0...release_v2.13.0)

## [Release 2.12.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.12.0)

### Features
- **auth:** Removed dependency on oauth2 token for refreshToken and move to initiateAuth for hostedUI ([#2543](https://github.com/aws-amplify/amplify-android/issues/2543))

### Bug Fixes
- **auth:** Fix for missing exception type for sign in ([#2567](https://github.com/aws-amplify/amplify-android/issues/2567))
- **datastore:** Prevent stop from interrupting subsequent start call ([#2569](https://github.com/aws-amplify/amplify-android/issues/2569))
- **datastore:** Ignore ApiAuthExceptions when subscribing or syncing models ([#2561](https://github.com/aws-amplify/amplify-android/issues/2561))
- add granular read ops enum ([#2265](https://github.com/aws-amplify/amplify-android/issues/2265))
- **flutter/datastore:** Nullable custom type arrays ([#2565](https://github.com/aws-amplify/amplify-android/issues/2565))
- **notifications:** also catch `RuntimeExecutionException` for google play store device use case ([#2562](https://github.com/aws-amplify/amplify-android/issues/2562))
 
[See all changes between 2.11.3 and 2.12.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.11.3...release_v2.12.0)

## [Release 2.11.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.11.3)

### Bug Fixes
- **storage:** Upgrade SDK to fix EOF exception ([#2558](https://github.com/aws-amplify/amplify-android/issues/2558))
- **data:** Resolve orchestrator transition threading issues. ([#2551](https://github.com/aws-amplify/amplify-android/issues/2551))

[See all changes between 2.11.2 and 2.11.3](https://github.com/aws-amplify/amplify-android/compare/release_v2.11.2...release_v2.11.3)

## [Release 2.11.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.11.2)

### Bug Fixes
- **datastore/flutter:** Custom type list serde ([#2545](https://github.com/aws-amplify/amplify-android/issues/2545))
- **auth:** Prevent credential continuation from returning multiple times. ([#2541](https://github.com/aws-amplify/amplify-android/issues/2541))

[See all changes between 2.11.1 and 2.11.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.11.1...release_v2.11.2)

## [Release 2.11.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.11.1)

### Bug Fixes
- **datastore:** Observe Query should not return deleted items, fixes - [#2069](https://github.com/aws-amplify/amplify-android/issues/2069) ([#2522](https://github.com/aws-amplify/amplify-android/issues/2522))
- **predictions:** send close frame when error occurs ([#2519](https://github.com/aws-amplify/amplify-android/issues/2519))
- **auth:** Fix for sending session expired when an invalid grant exception is received ([#2524](https://github.com/aws-amplify/amplify-android/issues/2524))
- **auth:** Fix for when authstate goes to an invalid state on confirmSignIn when it should accept the error state as is ([#2534](https://github.com/aws-amplify/amplify-android/issues/2534))
- **datastore:** Fix retry local after a deletion ([#2532](https://github.com/aws-amplify/amplify-android/issues/2532))

[See all changes between 2.11.0 and 2.11.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.11.0...release_v2.11.1)

## [Release 2.11.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.11.0)

### Features
- **logging:** Add Cloudwatch Logging Plugin ([#2518](https://github.com/aws-amplify/amplify-android/issues/2518))

[See all changes between 2.10.0 and 2.11.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.10.0...release_v2.11.0)

## [Release 2.10.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.10.0)

### Features
- **auth:** oidc plugin development toolkit ([#2507](https://github.com/aws-amplify/amplify-android/issues/2507))

[See all changes between 2.9.1 and 2.10.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.9.1...release_v2.10.0)

## [Release 2.9.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.9.1)

### Bug Fixes
- **auth:** Fix for confirm sign in state machine callback ([#2503](https://github.com/aws-amplify/amplify-android/issues/2503))
- **auth:** Fix for adding missing parameter for resolving device SRP ([#2508](https://github.com/aws-amplify/amplify-android/issues/2508))

[See all changes between 2.9.0 and 2.9.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.9.0...release_v2.9.1)

## [Release 2.9.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.9.0)

### Features
- **api:** Custom Primary Key API Support([#2470](https://github.com/aws-amplify/amplify-android/issues/2470))

### Bug Fixes
- **misc:** bumping kotlin sdk version to include enhanced proxy exception messages ([#2438](https://github.com/aws-amplify/amplify-android/issues/2438))
- **datastore:** DataStore CPK fixes([#2470](https://github.com/aws-amplify/amplify-android/issues/2470))

[See all changes between 2.8.7 and 2.9.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.7...release_v2.9.0)

## [Release 2.8.7](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.7)

### Bug Fixes
- **predictions:** Use English locale for date/time ([#2491](https://github.com/aws-amplify/amplify-android/issues/2491))

[See all changes between 2.8.6 and 2.8.7](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.6...release_v2.8.7)

## [Release 2.8.6](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.6)

### Bug Fixes
- **datastore:** Flutter: fail to serialize list of custom type values ([#2462](https://github.com/aws-amplify/amplify-android/issues/2462))

[See all changes between 2.8.5 and 2.8.6](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.5...release_v2.8.6)

## [Release 2.8.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.5)

### Miscellaneous
- @fix(data): Fix reachability debouncer causing delay on local inserts ([#2464](https://github.com/aws-amplify/amplify-android/issues/2464))

[See all changes between 2.8.4 and 2.8.5](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.4...release_v2.8.5)

## [Release 2.8.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.4)

### Bug Fixes
- **datastore:** send only distinct network events after subscribe ([#2451](https://github.com/aws-amplify/amplify-android/issues/2451))

[See all changes between 2.8.3 and 2.8.4](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.3...release_v2.8.4)

## [Release 2.8.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.3)

### Bug Fixes
- **datastore:** SerializedCustomType de/serialization ([#2350](https://github.com/aws-amplify/amplify-android/issues/2350))
- **auth:** Use java.time.Instant for AWSTemporaryCredentials expiration ([#2415](https://github.com/aws-amplify/amplify-android/issues/2415))
- **datastore:** Load pending mutations one at a time from outbox ([#2428](https://github.com/aws-amplify/amplify-android/issues/2428))
- **datastore:** register network callback only once in reachability monitor ([#2434](https://github.com/aws-amplify/amplify-android/issues/2434))
- **api:** Allow null associations on non-required fields. ([#2440](https://github.com/aws-amplify/amplify-android/issues/2440))

[See all changes between 2.8.2 and 2.8.3](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.2...release_v2.8.3)

## [Release 2.8.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.2)

### Miscellaneous
- fix(all) Fix non-existent aws-http version + ensure Smithy version matches one in Kotlin SDK ([#2425](https://github.com/aws-amplify/amplify-android/issues/2425))

[See all changes between 2.8.1 and 2.8.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.1...release_v2.8.2)
## [Release 2.8.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.1)

### Bug Fixes
- String interpolation for workflow ([#2412](https://github.com/aws-amplify/amplify-android/issues/2412))
- Restore notifying comments on pending-close-response issues ([#2417](https://github.com/aws-amplify/amplify-android/issues/2417))

### Miscellaneous
- Revert "fix(datastore): Fix MutationProcessorRetryTest" ([#2419](https://github.com/aws-amplify/amplify-android/issues/2419))

[See all changes between 2.8.0 and 2.8.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.8.0...release_v2.8.1)
## [Release 2.8.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.8.0)

### Features
- **storage:** Add pagination support for list api ([#2395](https://github.com/aws-amplify/amplify-android/issues/2395))

### Bug Fixes
- **datastore:** Release semaphore if an error occurs while starting or stopping the Orchestrator ([#2398](https://github.com/aws-amplify/amplify-android/issues/2398))
- **api:** Handle delete request with no body ([#2406](https://github.com/aws-amplify/amplify-android/issues/2406))
- **auth:** Prevent auth calls from processing until configured or errored ([#2402](https://github.com/aws-amplify/amplify-android/issues/2402))

[See all changes between 2.7.1 and 2.8.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.7.1...release_v2.8.0)

## [Release 2.7.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.7.1)

- There are no customer-impacting changes for this release. This release includes internal changes only.

[See all changes between 2.7.0 and 2.7.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.7.0...release_v2.7.1)

## [Release 2.7.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.7.0)

### Features
- **predictions:** Liveness ([#2381](https://github.com/aws-amplify/amplify-android/issues/2381))

### Bug Fixes
- **auth:** Fix Kotlin Facade fetchAuthSession options mapping ([#2384](https://github.com/aws-amplify/amplify-android/issues/2384))

[See all changes between 2.6.0 and 2.7.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.6.0...release_v2.7.0)

## [Release 2.6.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.6.0)

### Features
- **aws-datastore:** Make the Sync Engine pausable/resumable on connectivity changes ([#2306](https://github.com/aws-amplify/amplify-android/issues/2306))

### Bug Fixes
- **datastore:** Fix MutationProcessorRetryTest ([#2197](https://github.com/aws-amplify/amplify-android/issues/2197))
- **datastore:** Use a UnicastSubject instead of a ReplaySubject ([#2353](https://github.com/aws-amplify/amplify-android/issues/2353))
- **maplibre-adapter:** update MapLibre version ([#2370](https://github.com/aws-amplify/amplify-android/issues/2370))
- **predictions:** null check values returned from service ([#2377](https://github.com/aws-amplify/amplify-android/issues/2377))
- **api:** Use better fallback for list query v2 ([#2344](https://github.com/aws-amplify/amplify-android/issues/2344))

[See all changes between 2.5.0 and 2.6.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.5.0...release_v2.6.0)

## [Release 2.5.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.5.0)

### Features
- **notifications:** add push notifications category and plugin ([#2336](https://github.com/aws-amplify/amplify-android/pull/2336))

[See all changes between 2.4.1 and 2.5.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.4.1...release_v2.5.0)

## [Release 2.4.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.4.1)

### Bug Fixes
- **auth:** Add hosted ui error description on token failure ([#2338](https://github.com/aws-amplify/amplify-android/issues/2338))
- **api:** Use instr() for beginswith checking v2 ([#2347](https://github.com/aws-amplify/amplify-android/issues/2347))

[See all changes between 2.4.0 and 2.4.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.4.0...release_v2.4.1)

## [Release 2.4.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.4.0)

### Features
- **auth:** Add aws-core and AWSCredentialsProvider ([#2316](https://github.com/aws-amplify/amplify-android/issues/2316))

### Bug Fixes
- **core:** add null checks for dev menu logs ([#2332](https://github.com/aws-amplify/amplify-android/issues/2332))
- **datastore:** Accept applicable data if errors are present when syncing ([#2278](https://github.com/aws-amplify/amplify-android/issues/2278))
- **auth:** Fix for missing session variable when custom auth is performed ([#2335](https://github.com/aws-amplify/amplify-android/issues/2335))

[See all changes between 2.3.0 and 2.4.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.3.0...release_v2.4.0)

## [Release 2.3.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.3.0)

### Features
- **storage:** Add support for S3 acceleration mode ([#2304](https://github.com/aws-amplify/amplify-android/issues/2304))
- **aws-datastore:** Make the reachability component configurable ([#2307](https://github.com/aws-amplify/amplify-android/issues/2307))
- **aws-api,aws-datastore:** WebSocket improvements ([#2283](https://github.com/aws-amplify/amplify-android/issues/2283))

### Bug Fixes
- **datastore:** Fix aliasing of column names ([#2312](https://github.com/aws-amplify/amplify-android/issues/2312))
- **auth:** Delete user invalid state fixes ([#2326](https://github.com/aws-amplify/amplify-android/issues/2326))

### Miscellaneous
- Restore publishing sources jar ([#2329](https://github.com/aws-amplify/amplify-android/issues/2329))

[See all changes between 2.2.2 and 2.3.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.2.2...release_v2.3.0)

## [Release 2.2.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.2.2)

### Bug Fixes
- **auth:** fix npe in initialize fetch auth session ([#2284](https://github.com/aws-amplify/amplify-android/issues/2284))
- **auth:** Fix confirm signin when incorrect MFA code is entered ([#2286](https://github.com/aws-amplify/amplify-android/issues/2286))

[See all changes between 2.2.1 and 2.2.2](https://github.com/aws-amplify/amplify-android/compare/release_v2.2.1...release_v2.2.2)

## [Release 2.2.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.2.1)

### Bug Fixes
- **auth:** Moving credential provider to main (#2273)

[See all changes between 2.2.0 and 2.2.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.2.0...release_v2.2.1)

## [Release 2.2.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.2.0)

### Features
- **auth:** add required hash param to cognito api calls (#2266)
- **datastore:** Add recoverability improvements (#2201)
- **auth:** Added parity test for fetchDevices,rememberDevice,forgetDevice and fetchUserAttributes (#2174)

### Bug Fixes
- **analytics:** Remove test dependencies from implementation configuration (#2253)
- **auth:** Fix Authorization header for HostedUI fetchToken when appSecret is used (#2264)

[See all changes between 2.1.1 and 2.2.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.1.1...release_v2.2.0)

## [Release 2.1.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.1.1)

### Bug Fixes
- **datastore:** Fix lock contention issue when running DataStore.start() from the callback of DataStore.stop() (#2208)
- **core:** Remove unused dependencies (#2207)
- **geo:** Bump MapLibre SDK to 9.6.0 (#2254)

[See all changes between 2.1.0 and 2.1.1](https://github.com/aws-amplify/amplify-android/compare/release_v2.1.0...release_v2.1.1)

## [Release 2.1.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.1.0)

### Features
- feat(Geo): Add Kotlin Geo Facade (#2155)
- Add a network status listener to restart DataStore after the network comes back online. (#2148)
- feat(auth): Overriding sign in when the State machine is already in the signing in state (#2187)

### Miscellaneous
- chore: Remove deprecated maven plugin (#2137)
- chore: Remove Javadoc tasks (#2139)
- Update README.md (#2120)
- Dengdan stress test (#2153)
- Feat(Auth Test): Custom party testing for Custom Test without SRP (#2149)
- Unignore storage and pinpoint tests (#2156)
- Update DeviceFarm build config (#2168)
- Add Geo Rx Bindings (#2159)
- chore: Re-add storage tests (#2163)
- chore: Upgrade Gradle, AGP, and KtLint (#2172)
- Add a buildspec file for nightly tests (#2180)
- Chore(Auth): Implementation of the custom auth with SRP parity testing use case (#2167)
- chore: Add PR checker workflow (#2188)
- fix(auth): Fix for when loading credentials the success/error is fired twice (#2184)

### Bug fixes
- fix(core): remove unused dynamic nav dependency (#2132)
- fix(datastore): remove typename from ModelMetadata (#2122)
- fix: Change order of updating state in local cache (#2141)
- fix: fix integration test and added logger to integration test (#2143)
- Fix for when move to idle state is called twice (#2152)
- Fix(Auth): Sign up if successful should return DONE instead of Confirm sign up (#2130)
- fix: Add missing apis in storage Kotlin & RxJava facade (#2160)
- fix: user metadata was persisted empty in the database (#2165)
- fix(geo): Increase Geo timeout so that it runs successfully on a Pixel 3a XL (#2177)

[See all changes between 2.1.0 and 2.0.0](https://github.com/aws-amplify/amplify-android/compare/release_v2.0.0...release_v2.1.0)

## [Release 2.0.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v2.0.0)

###Breaking Changes

#### Android SDK
- Support for **Android SDK API 24: Android 7.0 (Nougat) and higher**

#### Escape Hatches
- Escape Hatches provide access to AWS SDK for Kotlin instead of `AWSMobileClient` from AWS SDK for Android.

#### Auth
- `signIn` now returns result with `isSignedIn` instead of `isSignInComplete`
- `confirmResetPassword` API takes additional `username` parameter.
- `signOut` now takes single `onComplete` parameter instead of `onSuccess` and `onError`.
- `fetchAuthSession` now returns `identityIdResult` instead of `identityId`.
- `getCurrentUser` API is now asynchronous and requires `onSuccess` and `onError` parameters. `AuthUser` is returned in `onSuccess`
- The escape hatch now provides access to the underlying `CognitoIdentityProviderClient` and `CognitoIdentityClient` instance.
- Parameters `signInQueryParameters`, `signOutQueryParameters`, and `tokenQueryParameters` are dropped from `AuthWebUISignInOptions`.
- `federationProviderName` has been dropped from `AWSCognitoAuthWebUISignInOptions`.
- `signIn` will now return an error if you attempt to call sign in, while already signed in.

### Features
Replace underlying AWS SDK with AWS SDK for Kotlin.

#### Auth
- Federate to Identity Pool
- Custom auth flow now supports without SRP flow
- Supports user migration flow
- Force refresh token.

#### Storage
- Add support to query local enqueued transfers.

### Miscellaneous
- All the categories use the same version number

[See all changes between 2.0.0 and 1.37.6](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.6...release_v2.0.0)

## [Release 1.37.6](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.6)

### Miscellaneous
- Add clientMetadata to signIn
- Update build.gradle (#2045)

[See all changes between 1.37.5 and 1.37.6](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.5...release_v1.37.6)

## [Release 1.37.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.5)

### Miscellaneous
- Update build.gradle (#1991)

[See all changes between 1.37.4 and 1.37.5](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.4...release_v1.37.5)

## [Release 1.37.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.4)

### Miscellaneous
- Publish Javadocs (Amplify version) (#1897)
- Update build.gradle (#1959)

[See all changes between 1.37.3 and 1.37.4](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.3...release_v1.37.4)

## [Release 1.37.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.3)

### Bug Fixes
- fix(data): disable failing test (#1922)

[See all changes between 1.37.2 and 1.37.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.2...release_v1.37.3)

## [Release 1.37.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.2)

### Bug Fixes
- **datastore**: Fix for Flutter backtick (#1866)

[See all changes between 1.37.1 and 1.37.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.1...release_v1.37.2)

## [Release 1.37.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.1)

### Bug Fixes
- **storage**: Remove startForegroundService in favor of binding service

### Miscellaneous
- Update AWS SDK for Android version

[See all changes between 1.37.0 and 1.37.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.0...release_v1.37.1)

## [Release 1.37.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.0)

### Features
- **datastore**: Implemented support for custom primary key (#1650)

### Bug Fixes
- **api:** allow post request with empty body (#1864)

[See all changes between 1.36.5 and 1.37.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.36.5...release_v1.37.0)

## [Release 1.36.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.5)

### Miscellaneous
- Updating the version to 2.50.1 for AWS SDK (#1861)
- Reduce the importance level of transfer channel for foreground service to prevent sound/vibrate (#1860)

[See all changes between 1.36.4 and 1.36.5](https://github.com/aws-amplify/amplify-android/compare/release_v1.36.4...release_v1.36.5)

## [Release 1.36.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.4)

### Miscellaneous
- Call to start TransferService regardless of if it has already been started
- Update transfer message in notification
- Update the Delete REST API to ensure it would work with and without a body (#1842)
- Chore(Release): Updating mobile client to the latest (#1847)

[See all changes between 1.36.3 and 1.36.4](https://github.com/aws-amplify/amplify-android/compare/release_v1.36.3...release_v1.36.4)

## [Release 1.36.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.3)

### Bug Fixes
- **api:** catch all exceptions when making rest request (#1827)

[See all changes between 1.36.2 and 1.36.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.36.2...release_v1.36.3)

## [Release 1.36.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.2)

### Miscellaneous
- Expand a catch clause to catch all (#1806)

[See all changes between 1.36.1 and 1.36.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.36.1...release_v1.36.2)

## [Release 1.36.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.1)

### Miscellaneous
- fix orchestrator failing if emitter is disposed (#1755)
- catch exceptions from processOutboxItem (#1743)
- ci: added canary workflow (#1770)

[See all changes between 1.36.0 and 1.36.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.36.0...release_v1.36.1)

## [Release 1.36.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.0)

### Features
- Add flushEvents HubEvent for analytics (#1792)

### Miscellaneous
- Update build.gradle

[See all changes between 1.35.7 and 1.36.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.7...release_v1.36.0)

## [Release 1.35.7](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.7)

### Miscellaneous
- Fix for adding data back to the delete request if it contains data as that is allowed for the Delete api (#1735)
- Fix/1485 : Fix for sending the session expired hub event when all credentials are expired (#1779)
- Updating build.gradle to include the latest version of the aws sdk (#1783)

[See all changes between 1.35.6 and 1.35.7](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.6...release_v1.35.7)

## [Release 1.35.6](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.6)

### Bug Fixes
- fix: start transfer service as foreground for >= API26 (#1759)

### Miscellaneous
- chore: update notify_release.yml (#1720)
- ignore flaky test (#1768)
- upgrade jmespath to version 1.6.1 (#1766)
- Create closed_issue_message.yml (#1754)
- Bump SDK version to 2.48.0 (#1773)

[See all changes between 1.35.5 and 1.35.6](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.5...release_v1.35.6)

## [Release 1.36.5-dev-preview.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.5-dev-preview.0)

### Miscellaneous
- Dev preview update and version bump (#1752)

[See all changes between 1.35.5 and 1.36.5-dev-preview.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.5...release_v1.36.5-dev-preview.0)

## [Release 1.35.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.5)

### Miscellaneous
- chore: update gson version (#1744)
- Update notify_comments.yml (#1746)
- Update SDK version in build.gradle (#1747)

[See all changes between 1.35.4 and 1.35.5](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.4...release_v1.35.5)

## [Release 1.35.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.4)

### Miscellaneous
- Update SDK version in build.gradle (#1741)

[See all changes between 1.35.3 and 1.35.4](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.3...release_v1.35.4)

## [Release 1.35.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.3)

### Bug Fixes
- When DateTimeParseException is not available for lower apis (#1701)

### Miscellaneous
- Version bumps (#1721)

[See all changes between 1.35.2 and 1.35.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.2...release_v1.35.3)

## [Release 1.36.0-dev-preview.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.36.0-dev-preview.0)

### ⚠ BREAKING CHANGES

- The escape hatch for dev-preview supported plugins will no longer return AWSMobileClient.

### Features

- **Auth**:
  - Redesigned AWS Cognito Plugin architecture using state machines.
  - Dependency on `AWSMobileClient` is replaced with [AWS SDK for Kotlin](https://github.com/awslabs/aws-sdk-kotlin).
- **Storage** :
  - Removed dependency on AWS TransferUtility and other improvements
- **API and DataStore**:
  - Dependency on `AWSMobileClient` is replaced with [AWS SDK for Kotlin](https://github.com/awslabs/aws-sdk-kotlin).

[See all changes between 1.35.2 and 1.36.0-dev-preview.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.2...release_v1.36.0-dev-preview.0)

## [Release 1.35.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.2)

### Bug Fixes

- Update logic to extract S3 keys from list api response (#1706)

[See all changes between 1.35.1 and 1.35.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.1...release_v1.35.2)

## [Release 1.35.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.1)

### Bug Fixes

- **aws-api:** double-encode plus in url path segments (#1252)

### Miscellaneous

- Conflict resolver fixes in comments. (#1681)
- Provide default message for GraphQLResponse.Error when null/missing (#1700)

[See all changes between 1.35.0 and 1.35.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.35.0...release_v1.35.1)

## [Release 1.35.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.35.0)

### Features

- **maplibre-adapter:** add clustering to map view (#1692)

### Miscellaneous

- updated the pull request template to include information about tests and documentation update (#1695)
- Adding a new feature request template to our repo (#1696)
- Updating version of aws sdk (#1698)

[See all changes between 1.34.0 and 1.35.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.34.0...release_v1.35.0)

## [Release 1.34.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.34.0)

### Features

- Add authFlow type in signInOptions (#1686)

### Bug Fixes

- **datastore:** compare datetime values in consistent format when querying (#1670)

### Miscellaneous

- Connectivity crash fix (#1688)
- [aws-api] Fix DELETE rest calls not working with IamRequestDecorator (#1684)

[See all changes between 1.33.0 and 1.34.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.33.0...release_v1.34.0)

## [Release 1.33.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.33.0)

### Features

- **auth:** Add deleteUser API (#1656)

[See all changes between 1.32.2 and 1.33.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.32.2...release_v1.33.0)

## [Release 1.32.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.32.2)

### Miscellaneous

- Update notify_comments.yml (#1675)
- Remove the UUID restriction from persistentRecord (#1678)
- conflict resolver retry local fix (#1634)

[See all changes between 1.32.1 and 1.32.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.32.1...release_v1.32.2)

## [Release 1.32.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.32.1)

### Bug Fixes

- **datastore:** SerializedModel returns null for non-nullable list field (#1665)
- **datastore:** Remove timeout for hydrating sync processor in orchestrator. (#1658)

### Miscellaneous

- Update notify_comments.yml (#1671)

[See all changes between 1.32.0 and 1.32.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.32.0...release_v1.32.1)

## [Release 1.32.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.32.0)

### Features

- Add support for custom prefix resolver (#1659)

### Miscellaneous

- Update notify_comments.yml (#1654)
- Updating the AWS SDK to 2.41.0 (#1662)

[See all changes between 1.31.3 and 1.32.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.31.3...release_v1.32.0)

## [Release 1.31.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.31.3)

### Miscellaneous

- Update build.gradle (#1644)
- Adding Dokka to the core-kotlin module (#1645)
- Update build.gradle (#1652)

[See all changes between 1.31.2 and 1.31.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.31.2...release_v1.31.3)

## [Release 1.31.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.31.2)

### Bug Fixes

- **aws-api-appsync:** update getModelFields for flutter support (#1611)
- **api:** create RequestDecorator in interceptor (#1623)
- **api:** prevent thread blocking on subx cancellation (#1482)

[See all changes between 1.31.1 and 1.31.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.31.1...release_v1.31.2)

## [Release 1.31.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.31.1)

### Bug Fixes

- **datastore:** Alias table names and make column aliases unique in query (#1603)
- **aws-datastore:** halt cascading delete if foreign key not found (#1614)
- **maplibre-adapter** adjust pop-up elevation (#1601)

### Miscellaneous

- Bump SDK version in build.gradle (#1619)

[See all changes between 1.31.0 and 1.31.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.31.0...release_v1.31.1)

## [Release 1.31.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.31.0)

### Features

- **maplibre-adapter:** amplify map view with markers and search capabilities (#1598)

[See all changes between 1.30.1 and 1.31.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.30.1...release_v1.31.0)

## [Release 1.30.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.30.1)

### Bug Fixes

- **aws-api-appsync:** include nested belongsTo object in query selection set (#1585)
- **maplibre-adapter:** add content attribution info to map view (#1591)

[See all changes between 1.30.0 and 1.30.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.30.0...release_v1.30.1)

## [Release 1.30.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.30.0)

### Features

- **maplibre-adapter:** MapLibreView and Geo adapter integration (#1568)

### Bug Fixes

- **api:** GraphQL operation is now launched from a new thread (#1562)

[See all changes between 1.29.1 and 1.30.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.29.1...release_v1.30.0)

## [Release 1.29.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.29.1)

### Bug Fixes

- **api:** Add null check before cancelling Call object. (#1570)

### Miscellaneous

- Update build.gradle (#1578)

[See all changes between 1.29.0 and 1.29.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.29.0...release_v1.29.1)

## [Release 1.29.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.29.0)

### Features

- **geo:** Add search APIs (#1558)
- **api:** Add support for custom GraphQL endpoints. (#1564)

### Bug Fixes

- **datastore:** specify model name when querying with Where.id (#1548)

[See all changes between 1.28.3 and 1.29.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.28.3...release_v1.29.0)

## [Release 1.28.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.28.3)

### Bug Fixes

- **geo:** Specify jvm target for kotlin package (#1546)
- **api:** replace pluralName with listPluralName & syncPluralName (#1523)
- **datastore:** Allow different model types with same ID (#1541)

### Miscellaneous

- Update build.gradle (#1553)

[See all changes between 1.28.2 and 1.28.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.28.2...release_v1.28.3)

## [Release 1.28.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.28.2)

### Miscellaneous

- fix(datastore):predicate handling for observe (#1537)

[See all changes between 1.28.1 and 1.28.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.28.1...release_v1.28.2)

## [Release 1.28.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.28.1)

### Miscellaneous

- Observe query updates (#1520)
- Update AWS SDK ver to 2.33.0 (#1526)

[See all changes between 1.28.0 and 1.28.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.28.0...release_v1.28.1)

## [Release 1.28.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.28.0)

### Features

- **datastore:** Add support for parsing match none predicate in AppSync request builder (#1515)
- **datastore:** Add support for observe query (#1470)

### Bug Fixes

- **datastore:** timeout period not increasing for flutter (#1505)
- **datastore:** Ensure not to parse SerializedCustomType if value is null (#1513)
- **datastore:** Fix for issue with foreign keys on schema upgrade delete (#1501)

### Miscellaneous

- better announce which schema is failing to sync (#1479)

[See all changes between 1.27.0 and 1.28.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.27.0...release_v1.28.0)

## [Release 1.27.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.27.0)

### Features

- **geo:** Added support for Geo category (#1502)

### Bug Fixes

- **datastore:** ensure attaching nested model schema to SerializedModel (#1495)

[See all changes between 1.26.0 and 1.27.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.26.0...release_v1.27.0)

## [Release 1.26.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.26.0)

### Features

- **datastore:** Add non-model type support for amplify-flutter (#1459)

### Bug Fixes

- **auth:** check for correct exception type when signing out globally (#1473)
- **auth:** null-check username when getting current user (#1490)

[See all changes between 1.25.1 and 1.26.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.25.1...release_v1.26.0)

## [Release 1.25.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.25.1)

### Miscellaneous

- fix(predictions):remove invalid test (#1476)
- chore: SDK version bump

[See all changes between 1.25.0 and 1.25.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.25.0...release_v1.25.1)

## [Release 1.25.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.25.0)

### Features

- **datastore:** Added logic to retry on sync failure. (#1414)

[See all changes between 1.24.1 and 1.25.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.24.1...release_v1.25.0)

## [Release 1.24.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.24.1)

### Bug Fixes

- handle null values for predicates (#1435)

[See all changes between 1.24.0 and 1.24.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.24.0...release_v1.24.1)

## [Release 1.24.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.24.0)

### Features

- **auth:** add options to resendSignUpCode (#1422)
- **auth:** add options to resetPassword and confirmResetPassword (#1426)

### Bug Fixes

- **api:** expose selectionSet in request builder (#1440)
- check for canceled call to fix RxJava crash (#1441)

[See all changes between 1.23.0 and 1.24.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.23.0...release_v1.24.0)

## [Release 1.23.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.23.0)

### Features

- add support for AWS_LAMBDA auth type (#1412)

### Miscellaneous

- Delete stale.yml (#1421)
- Updated DataStore delete test based on expected delete behavior (#1423)
- feat(api) add CUSTOM case to AuthStrategy (#1428)

[See all changes between 1.22.0 and 1.23.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.22.0...release_v1.23.0)

## [Release 1.22.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.22.0)

### Features

- **aws-auth-cognito:** Adds clientMetadata to AWSCognitoAuthSignUpOptions (#1407)

[See all changes between 1.21.0 and 1.22.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.21.0...release_v1.22.0)

## [Release 1.21.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.21.0)

### Features

- **datastore:** Return nested data for belongsTo associations in datastore (#1390)

### Bug Fixes

- **analytics:** allow user attributes in identifyUser (#1306)

[See all changes between 1.20.1 and 1.21.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.20.1...release_v1.21.0)

## [Release 1.20.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.20.1)

### Bug Fixes

- increase timeout for subscriptions to be established on slow networks (#1389)
- **api:** move error handling to multi-auth operation (#1399)

[See all changes between 1.20.0 and 1.20.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.20.0...release_v1.20.1)

## [Release 1.20.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.20.0)

### Features

- **datastore:** adding multiauth support (#1347)

### Bug Fixes

- **datastore:** Merge mutations when create is followed by update (#1384)
- **datastore:** explicitly include id field for update mutations, to support types with custom primary keys (#1385)

### Miscellaneous

- Update SDK version to 2.26.0 (#1386)

[See all changes between 1.19.0 and 1.20.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.19.0...release_v1.20.0)

## [Release 1.19.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.19.0)

### Features

- add supporting types for multi-auth (#1346)

### Bug Fixes

- **auth:** throw correct auth exception for code mismatch (#1370)
- **datastore:** fix subscription timeout period not increasing (#1376)
- **datastore:** Add support for SerializedModel to predicate evaluation (#1375)
- **datastore:** merge incoming mutation with existing update mutation (#1379)

### Miscellaneous

- chore(api):tweaks to the api init process (#1309)
- Update stale.yml (#1380)

[See all changes between 1.18.0 and 1.19.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.18.0...release_v1.19.0)

## [Release 1.18.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.18.0)

### Features

- **aws-auth-cognito:** Allows userattributes in confirmSignIn (#1343)

[See all changes between 1.17.8 and 1.18.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.8...release_v1.18.0)

## [Release 1.17.8](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.17.8)

### Bug Fixes

- **auth:** Add ConfirmSignUpOptions for confirmSignUp API method (#1357)
- **storage:** remove duplicate error callback (#1366)

[See all changes between 1.17.7 and 1.17.8](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.7...release_v1.17.8)

## [Release 1.17.7](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.17.7)

### Bug Fixes

- **api:** check for null ModelSchema to prevent crash in SerializedModel toString method (#1341)
- **api:** default to mobile client for iam auth mode (#1351)
- **Auth:** prevent multiple invocations of success callback for updateUserAttributes (#1339)

### Miscellaneous

- refactor:add enum to represent auth rule provider (#1320)

[See all changes between 1.17.6 and 1.17.7](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.6...release_v1.17.7)

## [Release 1.17.6](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.17.6)

### Bug Fixes

- checkstyle failure on Windows (#1326)
- **datastore:** save metadata when merging even if mutation outbox has pending item (#1319)
- **datastore:** add syncExpression method to configuration builder that takes the modelName as a String (#1330)

[See all changes between 1.17.5 and 1.17.6](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.5...release_v1.17.6)

## [Release 1.17.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.17.5)

### Bug Fixes

- signed in hub event is now fired after currentUser is set, instead of before (#1300)
- **datastore,api:** Update and delete mutations now work when custom primary key is defined (#1292)

[See all changes between 1.17.4 and 1.17.5](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.4...release_v1.17.5)

## [Release 1.17.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.17.4)

### Bug Fixes

- Use ObjectsCompat since Objects is API 19+ (#1289)
- adds ConfirmSignInOptions for confirmSignIn API method (#1297)

### Miscellaneous

- Upgrade dependency on the latest SDK version (#1294, #1296)

[See all changes between 1.17.3 and 1.17.4](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.3...release_v1.17.4)

# Release 1.17.3

### Bug Fixes

- **datastore:** optimize sync queries with predicates by wrapping in an AND group (#1225)
- **kotlin:** getCurrentUser return type should be nullable (#1265)
- throws AlreadyConfiguredException when configured == true (#1274)

[See all changes between 1.17.2 and 1.17.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.17.2...release_v1.17.3)
