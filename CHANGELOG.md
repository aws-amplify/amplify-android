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
