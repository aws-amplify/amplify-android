## [Release 1.23.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.23.0)

### Features
- add support for AWS_LAMBDA auth type (#1412)

### Miscellaneous
- Delete stale.yml (#1421)
- Updated DataStore delete test based on expected delete behavior (#1423)

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
