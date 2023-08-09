## [Release 1.38.8](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.8)

### Miscellaneous
- Updated aws sdk version ([#2546](https://github.com/aws-amplify/amplify-android/issues/2546))

[See all changes between 1.38.7 and 1.38.8](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.7...release_v1.38.8)

## [Release 1.38.7](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.7)

### Bug Fixes
- Replace sha1prng secure random with recommended algorithm ([#3314](https://github.com/aws-amplify/aws-sdk-android/issues/3314))

[See all changes between 1.38.6 and 1.38.7](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.6...release_v1.38.7)

## [Release 1.38.6](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.6)

### Bug Fixes
- **datastore:** Flutter: fail to serialize list of custom type values ([#2463](https://github.com/aws-amplify/amplify-android/issues/2463))
- **chore:** Strip non-permitted characters by okhttp in headers ([#2473](https://github.com/aws-amplify/amplify-android/issues/2473))

[See all changes between 1.38.5 and 1.38.6](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.5...release_v1.38.6)

## [Release 1.38.5](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.5)

### Bug Fixes
- **api:** Handle delete request with no body ([#2405](https://github.com/aws-amplify/amplify-android/issues/2405))
- **datastore:** SerializedCustomType de/serialization ([#2363](https://github.com/aws-amplify/amplify-android/issues/2363))
- **datastore:** Load pending mutations one at a time from outbox (v1) ([#2429](https://github.com/aws-amplify/amplify-android/issues/2429))

### Miscellaneous
- Enable TLSv1.2 support for pre Android Lollipop devices ([#3258](https://github.com/aws-amplify/aws-sdk-android/issues/3258))

[See all changes between 1.38.4 and 1.38.5](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.4...release_v1.38.5)

## [Release 1.38.4](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.4)

### Bug Fixes
- **predictions:** Fix processing for table title blocks ([#2407](https://github.com/aws-amplify/amplify-android/issues/2407))

[See all changes between 1.38.3 and 1.38.4](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.3...release_v1.38.4)

## [Release 1.38.3](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.3)

### Bug Fixes
- **maplibre-adapter:** update MapLibre version ([#2371](https://github.com/aws-amplify/amplify-android/issues/2371))

### Miscellaneous
- Use better fallback for list query v1 ([#2345](https://github.com/aws-amplify/amplify-android/issues/2345))

[See all changes between 1.38.2 and 1.38.3](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.2...release_v1.38.3)

## [Release 1.38.2](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.2)

### Bug Fixes
- **core:** add null checks for dev menu logs ([#2333](https://github.com/aws-amplify/amplify-android/issues/2333))
- **api:** Use instr() for string checking v1 ([#2346](https://github.com/aws-amplify/amplify-android/issues/2346))

[See all changes between 1.38.1 and 1.38.2](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.1...release_v1.38.2)

## [Release 1.38.1](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.1)

### Bug Fixes
- **core:** Remove unused dependencies ([#2237](https://github.com/aws-amplify/amplify-android/issues/2237))
- **datastore:** Fix aliasing of column names ([#2310](https://github.com/aws-amplify/amplify-android/issues/2310))

[See all changes between 1.38.0 and 1.38.1](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.0...release_v1.38.1)

## [Release 1.38.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.38.0)

### Features
- **datastore:** Add recoverability improvements (v1) (#2268)

### Miscellaneous
- bump sdk version (#2271). See [AWS SDK for Android 2.63.0 Release](https://github.com/aws-amplify/aws-sdk-android/releases/tag/release_v2.63.0)


[See all changes between 1.37.13 and 1.38.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.13...release_v1.38.0)

## [Release 1.37.13](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.13)

### Miscellaneous
- Update build.gradle (#2246)

[See all changes between 1.37.12 and 1.37.13](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.12...release_v1.37.13)

## [Release 1.37.12](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.12)

### Bug Fixes
- **Storage:** Prevent re-binding transferService after it is initialized (#2211)

### Miscellaneous
- Update build.gradle (#2241)

[See all changes between 1.37.11 and 1.37.12](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.11...release_v1.37.12)

## [Release 1.37.11](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.11)

### Bug Fixes
- **datastore:** Fix lock contention issue when running DataStore.start() from the callback of DataStore.stop() (#2209)

### Miscellaneous
- Increase Geo timeout so that it runs more consistently (#2181)

[See all changes between 1.37.10 and 1.37.11](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.10...release_v1.37.11)

## [Release 1.37.10](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.10)

### Bug Fixes
- **core:** remove unused dynamic nav dependency (#2131)
- **datastore:** remove typename from ModelMetadata (#2122) (#2166)

### Miscellaneous
- Update DeviceFarm build config (#2169)
- bump sdk version (#2175)

[See all changes between 1.37.9 and 1.37.10](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.9...release_v1.37.10)

## [Release 1.37.9](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.9)

### Features
- Update build.gradle (#2134)

[See all changes between 1.37.8 and 1.37.9](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.8...release_v1.37.9)

## [Release 1.37.8](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.8)

### Miscellaneous
- Update build.gradle (#2118)

[See all changes between 1.37.7 and 1.37.8](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.7...release_v1.37.8)

## [Release 1.37.7](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.37.7)

### Bug Fixes
- **amplify-tools:** use projectDir for project.file relative paths (#2011)

### Miscellaneous
- replace jcenter with mavenCentral
- Update build.gradle (#2101)
- Update release_pr.yml (#2106)

[See all changes between 1.37.6 and 1.37.7](https://github.com/aws-amplify/amplify-android/compare/release_v1.37.6...release_v1.37.7)

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
