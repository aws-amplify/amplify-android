## [Release 0.21.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.21.1)

### Miscellaneous
- Moving TransferService responsibility to Amplify to track when to safely stop
- Set AmplifyTransferService to internal
- correct logic to stop transfer service
- Ensure startForeground is always called after startForegroundService
- Replace problematic startForegroundService with bind.
- Removing ignore from tests
- ktlint
- Update storage tests and add stress test
- fix import
- start unbind check in onServiceConnected to ensure handler is started on first transfer
- pr comments

[See all changes between 0.21.0 and 0.21.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.21.0...release-kotlin_v0.21.1)

## [Release 0.21.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.21.0)

### Features
- **datastore**: Implemented support for custom primary key (#1650)

### Bug Fixes
- **api:** allow post request with empty body (#1864)

[See all changes between 0.20.5 and 0.21.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.20.5...release-kotlin_v0.21.0)

## [Release 0.20.5](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.20.5)

### Miscellaneous
- Updating the version to 2.50.1 for AWS SDK (#1861)
- Reduce the importance level of transfer channel for foreground service to prevent sound/vibrate (#1860)

[See all changes between 0.20.4 and 0.20.5](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.20.4...release-kotlin_v0.20.5)

## [Release 0.20.4](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.20.4)

### Miscellaneous
- Call to start TransferService regardless of if it has already been started
- Update transfer message in notification
- Update the Delete REST API to ensure it would work with and without a body (#1842)
- Chore(Release): Updating mobile client to the latest (#1847)

[See all changes between 0.20.3 and 0.20.4](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.20.3...release-kotlin_v0.20.4)

## [Release 0.20.3](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.20.3)

### Bug Fixes
- **api:** catch all exceptions when making rest request (#1827)

[See all changes between 0.20.2 and 0.20.3](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.20.2...release-kotlin_v0.20.3)

## [Release 0.20.2](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.20.2)

### Miscellaneous
- Expand a catch clause to catch all (#1806)

[See all changes between 0.20.1 and 0.20.2](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.20.1...release-kotlin_v0.20.2)

## [Release 0.20.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.20.1)

### Miscellaneous
- fix orchestrator failing if emitter is disposed (#1755)
- catch exceptions from processOutboxItem (#1743)
- ci: added canary workflow (#1770)

[See all changes between 0.20.0 and 0.20.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.20.0...release-kotlin_v0.20.1)

## [Release 0.20.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.20.0)

### Features
- Add flushEvents HubEvent for analytics (#1792)

### Miscellaneous
- Update build.gradle

[See all changes between 0.19.7 and 0.20.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.7...release-kotlin_v0.20.0)

## [Release 0.19.7](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.7)

### Miscellaneous
- Fix for adding data back to the delete request if it contains data as that is allowed for the Delete api (#1735)
- Fix/1485 : Fix for sending the session expired hub event when all credentials are expired (#1779)
- Updating build.gradle to include the latest version of the aws sdk (#1783)

[See all changes between 0.19.6 and 0.19.7](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.6...release-kotlin_v0.19.7)

## [Release 0.19.6](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.6)

### Bug Fixes
- fix: start transfer service as foreground for >= API26 (#1759)

### Miscellaneous
- chore: update notify_release.yml (#1720)
- ignore flaky test (#1768)
- upgrade jmespath to version 1.6.1 (#1766)
- Create closed_issue_message.yml (#1754)
- Bump SDK version to 2.48.0 (#1773)

[See all changes between 0.19.5 and 0.19.6](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.5...release-kotlin_v0.19.6)

## [Release 0.19.5](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.5)

### Miscellaneous
- chore: update gson version (#1744)
- Update notify_comments.yml (#1746)
- Update SDK version in build.gradle (#1747)

[See all changes between 0.19.4 and 0.19.5](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.4...release-kotlin_v0.19.5)

## [Release 0.19.4](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.4)

### Miscellaneous
- Update SDK version in build.gradle (#1741)

[See all changes between 0.19.3 and 0.19.4](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.3...release-kotlin_v0.19.4)

## [Release 0.19.3](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.3)

### Bug Fixes
- When DateTimeParseException is not available for lower apis (#1701)

### Miscellaneous
- Version bumps (#1721)

[See all changes between 0.19.2 and 0.19.3](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.2...release-kotlin_v0.19.3)

## [Release 0.19.2](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.2)

### Bug Fixes
- Update logic to extract S3 keys from list api response (#1706)

[See all changes between 0.19.1 and 0.19.2](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.1...release-kotlin_v0.19.2)

## [Release 0.19.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.1)

### Bug Fixes
- **aws-api:** double-encode plus in url path segments (#1252)

### Miscellaneous
- Conflict resolver fixes in comments. (#1681)
- Provide default message for GraphQLResponse.Error when null/missing (#1700)

[See all changes between 0.19.0 and 0.19.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.19.0...release-kotlin_v0.19.1)

## [Release 0.19.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.19.0)

### Features
- **maplibre-adapter:** add clustering to map view (#1692)

### Miscellaneous
- updated the pull request template to include information about tests and documentation update (#1695)
- Adding a new feature request template to our repo (#1696)
- Updating version of aws sdk (#1698)

[See all changes between 0.18.0 and 0.19.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.18.0...release-kotlin_v0.19.0)

## [Release 0.18.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.18.0)

### Features
- Add authFlow type in signInOptions (#1686)

### Bug Fixes
- **datastore:** compare datetime values in consistent format when querying (#1670)

### Miscellaneous
- Connectivity crash fix (#1688)
- [aws-api] Fix DELETE rest calls not working with IamRequestDecorator (#1684)

[See all changes between 0.17.0 and 0.18.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.17.0...release-kotlin_v0.18.0)

## [Release 0.17.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.17.0)

### Features
- **auth:** Add deleteUser API (#1656)

[See all changes between 0.16.2 and 0.17.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.16.2...release-kotlin_v0.17.0)

## [Release 0.16.2](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.16.2)

### Miscellaneous
- Update notify_comments.yml (#1675)
- Remove the UUID restriction from persistentRecord (#1678)
- conflict resolver retry local fix (#1634)

[See all changes between 0.16.1 and 0.16.2](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.16.1...release-kotlin_v0.16.2)

## [Release 0.16.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.16.1)

### Bug Fixes
- **datastore:** SerializedModel returns null for non-nullable list field (#1665)
- **datastore:** Remove timeout for hydrating sync processor in orchestrator. (#1658)

### Miscellaneous
- Update notify_comments.yml (#1671)

[See all changes between 0.16.0 and 0.16.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.16.0...release-kotlin_v0.16.1)

## [Release 0.16.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.16.0)

### Features
- Add support for custom prefix resolver (#1659)

### Miscellaneous
- Update notify_comments.yml (#1654)
- Updating the AWS SDK to 2.41.0 (#1662)

[See all changes between 0.15.3 and 0.16.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.15.3...release-kotlin_v0.16.0)

## [Release 0.15.3](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.15.3)

### Miscellaneous
- Update build.gradle (#1644)
- Adding Dokka to the core-kotlin module (#1645)
- Update build.gradle (#1652)

[See all changes between 0.15.2 and 0.15.3](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.15.2...release-kotlin_v0.15.3)

## [Release 0.15.2](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.15.2)

### Bug Fixes
- **aws-api-appsync:** update getModelFields for flutter support (#1611)
- **api:** create RequestDecorator in interceptor (#1623)
- **api:** prevent thread blocking on subx cancellation (#1482)

[See all changes between 0.15.1 and 0.15.2](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.15.1...release-kotlin_v0.15.2)

## [Release 0.15.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.15.1)

### Bug Fixes
- **datastore:** Alias table names and make column aliases unique in query (#1603)
- **aws-datastore:** halt cascading delete if foreign key not found (#1614)
- **maplibre-adapter** adjust pop-up elevation (#1601)

### Miscellaneous
- Bump SDK version in build.gradle (#1619)

[See all changes between 0.15.0 and 0.15.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.15.0...release-kotlin_v0.15.1)

## [Release 0.15.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.15.0)

### Features
- **maplibre-adapter:** amplify map view with markers and search capabilities (#1598)

[See all changes between 0.14.1 and 0.15.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.14.1...release-kotlin_v0.15.0)

## [Release 0.14.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.14.1)

### Bug Fixes
- **aws-api-appsync:** include nested belongsTo object in query selection set (#1585)
- **maplibre-adapter:** add content attribution info to map view (#1591)

[See all changes between 0.14.0 and 0.14.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.14.0...release-kotlin_v0.14.1)

## [Release 0.14.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.14.0)

### Features
- **maplibre-adapter:** MapLibreView and Geo adapter integration (#1568)

### Bug Fixes
- **api:** GraphQL operation is now launched from a new thread (#1562)

[See all changes between 0.13.1 and 0.14.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.13.1...release-kotlin_v0.14.0)

## [Release 0.13.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.13.1)

### Bug Fixes
- **api:** Add null check before cancelling Call object. (#1570)

### Miscellaneous
- Update build.gradle (#1578)

[See all changes between 0.13.0 and 0.13.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.13.0...release-kotlin_v0.13.1)

## [Release 0.13.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.13.0)

### Features
- **geo:** Add search APIs (#1558)
- **api:** Add support for custom GraphQL endpoints. (#1564)

### Bug Fixes
- **datastore:** specify model name when querying with Where.id (#1548)

[See all changes between 0.12.3 and 0.13.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.12.3...release-kotlin_v0.13.0)

## [Release 0.12.3](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.12.3)

### Bug Fixes
- **geo:** Specify jvm target for kotlin package (#1546)
- **api:** replace pluralName with listPluralName & syncPluralName (#1523)
- **datastore:** Allow different model types with same ID (#1541)

### Miscellaneous
- Update build.gradle (#1553)

[See all changes between 0.12.2 and 0.12.3](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.12.2...release-kotlin_v0.12.3)

## [Release 0.12.2](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.12.2)

### Miscellaneous
- fix(datastore):predicate handling for observe (#1537)

[See all changes between 0.12.1 and 0.12.2](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.12.1...release-kotlin_v0.12.2)

## [Release 0.12.1](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.12.1)

### Miscellaneous
- Observe query updates (#1520)
- Update AWS SDK ver to 2.33.0 (#1526)

[See all changes between 0.12.0 and 0.12.1](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.12.0...release-kotlin_v0.12.1)

## [Release 0.12.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.12.0)

[See all changes between 0.11.0 and 0.12.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.11.0...release-kotlin_v0.12.0)

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
