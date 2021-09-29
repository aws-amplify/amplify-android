## [Release 0.11.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.11.0)

### Features
- **geo:** Added support for Geo category (#1502)

### Bug Fixes
- **datastore:** ensure attaching nested model schema to SerializedModel (#1495)

[See all changes between 0.10.0 and 0.11.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.10.0...release-kotlin_v0.11.0)

## [Release 0.10.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.10.0)

### Features
- **datastore:** Add non-model type support for amplify-flutter (#1459)

### Bug Fixes
- **auth:** check for correct exception type when signing out globally (#1473)
- **auth:** null-check username when getting current user (#1490)

[See all changes between 0.9.1 and 0.10.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.9.1...release-kotlin_v0.10.0)

## [Release 0.9.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.9.1)

### Miscellaneous
- fix(predictions):remove invalid test (#1476)
- chore: SDK version bump

[See all changes between 0.9.0 and 0.9.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.9.0...release-kotlin_v0.9.1)

## [Release 0.9.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.9.0)

### Features
- **datastore:** Retry sync (#1414)

[See all changes between 0.8.1 and 0.9.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.8.1...release-kotlin_v0.9.0)

## [Release 0.8.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.8.1)

### Bug Fixes
- handle null values for predicates (#1435)

[See all changes between 0.8.0 and 0.8.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.8.0...release-kotlin_v0.8.1)

## [Release 0.8.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.8.0)

### Features
- **auth:** add options to resendSignUpCode (#1422)
- **auth:** add options to resetPassword and confirmResetPassword (#1426)

### Bug Fixes
- **api:** expose selectionSet in request builder (#1440)
- check for canceled call to fix RxJava crash (#1441)

[See all changes between 0.7.0 and 0.8.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.7.0...release-kotlin_v0.8.0)

## [Release 0.7.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.7.0)

### Features
- add support for AWS_LAMBDA auth type (#1412)

### Miscellaneous
- Delete stale.yml (#1421)
- Updated DataStore delete test based on expected delete behavior (#1423)
- feat(api) add CUSTOM case to AuthStrategy (#1428)

[See all changes between 0.6.0 and 0.7.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.6.0...release-kotlin_v0.7.0)

## [Release 0.6.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.6.0)

### Features
- **aws-auth-cognito:** Adds clientMetadata to AWSCognitoAuthSignUpOptions (#1407)

[See all changes between 0.5.0 and 0.6.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.5.0...release-kotlin_v0.6.0)

## [Release 0.5.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.5.0)

### Features
- **datastore:** Return nested data for belongsTo associations in datastore (#1390)

### Bug Fixes
- **analytics:** allow user attributes in identifyUser (#1306)

[See all changes between 0.4.1 and 0.5.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.4.1...release-kotlin_v0.5.0)

## [Release 0.4.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.4.1)

### Bug Fixes
- increase timeout for subscriptions to be established on slow networks (#1389)
- **api:** move error handling to multi-auth operation (#1399)

[See all changes between 0.4.0 and 0.4.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.4.0...release-kotlin_v0.4.1)

## [Release 0.4.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.4.0)

### Features
- **datastore:** adding multiauth support (#1347)

### Bug Fixes
- **datastore:** Merge mutations when create is followed by update (#1384)
- **datastore:** explicitly include id field for update mutations, to support types with custom primary keys (#1385)

### Miscellaneous
- Update SDK version to 2.26.0 (#1386)

[See all changes between 0.3.0 and 0.4.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.3.0...release-kotlin_v0.4.0)

## [Release 0.3.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.3.0)

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

[See all changes between 0.2.0 and 0.3.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.2.0...release-kotlin_v0.3.0)

## [Release 0.2.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.2.0)

### Features
- **aws-auth-cognito:** Allows userattributes in confirmSignIn (#1343)

[See all changes between 0.1.9 and 0.2.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.9...release-kotlin_v0.2.0)

## [Release 0.1.9](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.1.9)

### Bug Fixes
- **auth:** Add ConfirmSignUpOptions for confirmSignUp API method (#1357)
- **storage:** remove duplicate error callback (#1366)

[See all changes between 0.1.8 and 0.1.9](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.8...release-kotlin_v0.1.9)

## [Release 0.1.8](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.1.8)

### Bug Fixes
- **api:** check for null ModelSchema to prevent crash in SerializedModel toString method (#1341)
- **Auth:** prevent multiple invocations of success callback for updateUserAttributes (#1339)

### Miscellaneous
- create stale bot GitHub action (#1337)
- refactor:add enum to represent auth rule provider (#1320)
- default to mobile client for iam auth mode (#1351)

[See all changes between 0.1.7 and 0.1.8](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.7...release-kotlin_v0.1.8)

## [Release 0.1.7](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.1.7)

### Bug Fixes
- checkstyle failure on Windows (#1326)
- **datastore:** save metadata when merging even if mutation outbox has pending item (#1319)
- **datastore:** add syncExpression method to configuration builder that takes the modelName as a String (#1330)

[See all changes between 0.1.6 and 0.1.7](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.6...release-kotlin_v0.1.7)

## [Release 0.1.6](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.1.6)

### Bug Fixes
- signed in hub event is now fired after currentUser is set, instead of before (#1300)
- **datastore,api:** Update and delete mutations now work when custom primary key is defined (#1292)

[See all changes between 0.1.5 and 0.1.6](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.5...release-kotlin_v0.1.6)

## [Release 0.1.5](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.1.5)

### Bug Fixes
- Use ObjectsCompat since Objects is API 19+ (#1289)
- adds ConfirmSignInOptions for confirmSignIn API method (#1297)

### Miscellaneous
- Upgrade dependency on the latest SDK version (#1294, #1296)

[See all changes between 0.1.4 and 0.1.5](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.4...release-kotlin_v0.1.5)

# Release 0.1.4

### Bug Fixes
- **datastore:** optimize sync queries with predicates by wrapping in an AND group (#1225)
- **kotlin:** getCurrentUser return type should be nullable (#1265)
- throws AlreadyConfiguredException when configured == true (#1274)


[See all changes between 0.1.3 and 0.1.4](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.1.3...release-kotlin_v0.1.4)
