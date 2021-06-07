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
