## [Release 0.13.0](https://github.com/aws-amplify/amplify-android/releases/tag/release-kotlin_v0.13.0)

### Features
- **rxbindings:** add binding for predictions (#548)
- **API:** Remove nextToken from ModelPagination since nextToken is never exposed (#557)
- **API:** Add AuthRule annotation to support @auth directive (#559)
- **rxbindings:** add bindings for auth (#579)
- **auth:** Adds user info to sign up result (#732)
- **auth:** Adds improved HostedUI functionality (#739)
- **aws-auth-cognito:** implement device tracking (#738)
- **devmenu:** Add developer menu
- **aws-datastore:** implement mutation outbox hub events (#762)
- **core:** filter developer menu logs by log level (#788)
- **core:** Adds option to disable dev menu (#821)
- **Auth:** Add user attribute operations (Fetch/Update/Verify/Confirm user attributes) (#798)
- **aws-api:** support owner-based auth with oidc (#906)
- **Auth:** Adds the ability to specify a custom browser for web UI sign in/out (#1030)
- **aws-datastore:** add mutation failed hub event (#976)
- **datastore:** support sorting by related models (#1141)
- a kotlin-centric facade for amplify android
- **aws-api:** Support Custom OkHttp Configurations (#1207)
- **aws-auth-cognito:** Allows userattributes in confirmSignIn (#1343)
- add supporting types for multi-auth (#1346)
- **datastore:** adding multiauth support (#1347)
- **datastore:** Multiauth integration tests (#1349)
- **datastore:** Return nested data for belongsTo associations in datastore (#1390)
- **aws-auth-cognito:** Adds clientMetadata to AWSCognitoAuthSignUpOptions (#1407)
- add support for AWS_LAMBDA auth type (#1412)
- **auth:** add options to resendSignUpCode (#1422)
- **auth:** add options to resetPassword and confirmResetPassword (#1426)
- **datastore:** Retry sync (#1414)
- **datastore:** Add non-model type support for amplify-flutter (#1459)
- **geo:** Added support for Geo category (#1502)
- **datastore:** Add support for parsing match none predicate in AppSync request builder (#1515)
- **geo:** Add search APIs (#1558)
- **api:** Add support for custom GraphQL endpoints. (#1564)

### Bug Fixes
- **amplify-tools:** Don't escape JSON on non-Windows platforms (#526)
- **api:** Fix #538 by providing a default empty byte array for REST payload (#539)
- **aws-api:** cleanups to subscription endpoint (#542)
- **core:** use sentinel for null query predicate (#540)
- dont run proguard only publish lib rules (#546)
- **Auth:** Adds invalidateTokens param to properly signout HostedUI (#570)
- **amplify-tools:** Add task to create javadoc jar (#530)
- **DataStore:** Fix for contains predicate (#584)
- create aws-appsync module, fix Datastore serialization bug (part 1) (#601)
- **aws-api:** Fix path with leading slash breaking REST (#600)
- **core:** Updated amazonaws.amplify package name to amplifyframework (#616)
- **DataStore:** remove unnecessary observeOn call (#612)
- update default repo version name (#641)
- **api:** now correctly signs requests with query string params (#658)
- daemon style lifecycle for orchestrator
- **aws-datastore:** address issue where primary key was not properly quoted (#720)
- **aws-datastore:** remove insert statement cache (#718)
- **datastore:** typo in sql keyword between (#729)
- **auth:** Updates to use fixed HostedUI (#734)
- **DataStore:** use a LinkedList and HashMap to improve the time comp… (#613)
- **datastore:** order hydration tasks topologically (#741)
- relax condition of translate assertion (#740)
- do not allow changing plugins after configure (#751)
- **aws-datastore:** selection set for nested custom types (#730)
- rxbindings readme v2 to v3 (#789)
- **core:** remove dev menu app icon
- **aws-datastore:** emit outbox status event on mutation process (#790)
- **API:** Fixes owner auth subscribe with email/phone sign-in (#818)
- **Auth:** Fixes guest / anonymous user access (#850)
- **aws-datastore:** make subscription timeout model count dependent (#858)
- **API:** Adds support for custom identity claim (#889)
- align datastore config builder methods to ios (#907)
- **api:** Add owner field to selection set if implicitly defined (#926)
- consider deletion predicate when requested (#949)
- **aws-datastore:** checkstyle and integration tests fix (#957)
- integ test instructions in contributing guide
- properly support flutter online scenarios (#961)
- **aws-predictions:** expose label instances' bounding boxes (#968)
- **aws-datastore:** kludge to support flutter connected models (#985)
- associated Flutter models can sync to cloud (#988)
- **aws-datastore:** keep observations active despite errors (#993)
- **aws-datastore:** Prevent NPE while inspecting GraphQL errors (#1000)
- honor deletions from AppSync when used by Flutter (#1004)
- **aws-datastore:** support temporal types in Flutter (#1001)
- fix field label in toString() of ModelField (#1006)
- comprehend deserializtion of primitive types (#1007)
- **aws-datastore:** idempotent deletions from merger (#1014)
- **aws-datastore:** null modelName in OutboxMutationProcessed (#1021)
- **aws-datastore:** Add owner to SelectionSet for Flutter (#1020)
- **amplify-tools:** Add --forcePush to init (#1025)
- **aws-datastore:** deleting nonexistent model instance no longer fails (#1033)
- **aws-datastore:** conditional insert no longer succeeds (#1035)
- **aws-datastore:** store schema float as a java double (#1040)
- **aws-datastore:** Resolves crash when going offline (#1039)
- **aws-datastore:** ignore foreign key error during sync (#1058)
- **aws-api:** dont fire error if rest op canceled (#1072)
- continue processing outbox items after removal (#1084)
- **aws-datastore:** publish each cascading delete (#1059)
- export proguard rules for consumers (#1103)
- **datastore:** dont use where.id() function (#1130)
- **core:** query field now stores model name (#1133)
- remove MultiDexApplication from DataStore manifest
- add a small initial delay to failing rxbinding test
- **core:** Improve error messaging when category configuration or initialization fails (#1154)
- Delete relationships fix for flutter (#1168)
- **datastore:** save root instance instead of creating multiple iterators (#1171)
- **rxbindings:** don't emit errors to disposed subscriber
- **datastore:** fix crash when deleting model with many children (#1121)
- **datastore:** ignore sqlite exceptions during cascading delete (#1149)
- Add support for readonly fields such as createdAt, updatedAt (#1249)
- **aws-api:** use default cognito provider if needed. (#1254)
- **datastore:** optimize sync queries with predicates by wrapping in an AND group (#1225)
- **kotlin:** getCurrentUser return type should be nullable (#1265)
- **ci:** update branch name and PR subject (#1276)
- change protocol for github import (#1284)
- throws AlreadyConfiguredException when configured == true (#1274)
- Use ObjectsCompat since Objects is API 19+ (#1289)
- adds ConfirmSignInOptions for confirmSignIn API method (#1297)
- signed in hub event is now fired after currentUser is set, instead of before (#1300)
- **datastore,api:** Update and delete mutations now work when custom primary key is defined (#1292)
- checkstyle failure on Windows (#1326)
- **datastore:** save metadata when merging even if mutation outbox has pending item (#1319)
- **datastore:** add syncExpression method to configuration builder that takes the modelName as a String (#1330)
- **api:** check for null ModelSchema to prevent crash in SerializedModel toString method (#1341)
- **Auth:** prevent multiple invocations of success callback for updateUserAttributes (#1339)
- **auth:** Add ConfirmSignUpOptions for confirmSignUp API method (#1357)
- **storage:** remove duplicate error callback (#1366)
- **auth:** throw correct auth exception for code mismatch (#1370)
- **datastore:** fix subscription timeout period not increasing (#1376)
- **datastore:** Add support for SerializedModel to predicate evaluation (#1375)
- **datastore:** merge incoming mutation with existing update mutation (#1379)
- **datastore:** Merge mutations when create is followed by update (#1384)
- **datastore:** explicitly include id field for update mutations, to support types with custom primary keys (#1385)
- increase timeout for subscriptions to be established on slow networks (#1389)
- **api:** move error handling to multi-auth operation (#1399)
- **analytics:** allow user attributes in identifyUser (#1306)
- **api:** expose selectionSet in request builder (#1440)
- check for canceled call to fix RxJava crash (#1441)
- handle null values for predicates (#1435)
- **auth:** check for correct exception type when signing out globally (#1473)
- **auth:** null-check username when getting current user (#1490)
- **datastore:** ensure attaching nested model schema to SerializedModel (#1495)
- **datastore:** timeout period not increasing for flutter (#1505)
- **datastore:** Ensure not to parse SerializedCustomType if value is null (#1513)
- **geo:** Specify jvm target for kotlin package (#1546)
- **api:** replace pluralName with listPluralName & syncPluralName (#1523)
- **datastore:** Allow different model types with same ID (#1541)
- **datastore:** specify model name when querying with Where.id (#1548)
- **api:** Add null check before cancelling Call object. (#1570)

### Miscellaneous
- Creating initial file from template
- Creating initial file from template
- Creating initial file from template
- Updating initial README.md from template
- Creating initial file from template
- Creating initial file from template
- Initial commit of Amplify Core APIs
- Add Stub for Analytics Category
- Rename provider to plugin and add API Reference
- Refactor client API and provider API; Define ClientBehavior; Create Exception objects
- Add API Reference for Analytics
- Add partial implementation for AmplifyConfiguration and API References
- Adding gitignore
- Remove local.properties from the repository
- Remove some more user-specific files
- Remove files
- Fix compilation issues in pinpoint plugin
- [Core] Changed naming and signature for Catgeory and Plugin
- [Analytics] Changed naming and signature for Category and Plugin
- Update core
- [Storage] Sync-up with new core design
- Removed .iml files and .gradle artifact folder
- Reverted unintended name changes in sample app
- [Core] Complete implementation for addPlugin and removePlugin
- [Storage] Add JavaDoc
- [Storage] Overload API with callback register and remove it from AsyncOperation
- [Core] Support addPugin with config and created module for S3
- [Core] Fix addPlugin with configuration and plugin.configure
- [Storage] Fix addPlugin with configuration and plugin.configure
- [Core] Overloaded configure method on Plugin layer
- [Core] Add JavaDoc to Exceptions; Add optional recovery suggestion message
- Gitignore cleanup
- [DataStore] Add Stub
- [Storage] Added overloaded API signatures
- [Analytics] Remove assert statements
- [Core] Refactor AsyncOperation
- [Core] Remove unused classes
- [Storage] Propagate core async update
- [Analytics] Propagate changes to plugin implementation
- [Core] Add Stub for API, Logging and Hub
- [Core] Remove configure(context) from category and plugin [Core] Finish implementation of methods in Amplify class
- [Analytics] Add default plugin logic
- [Core] Remove configure(context, envrionemnt)
- [Api] infrastructure for plugin
- [Api] Add getSelectedPlugin()
- Appended suffix to category interfaces
- [Core] Updated configure method
- Bump Gradle version & strip Kotlin build support
- [SampleApp] Project can build
- [Core] Changed {Category}Plugin.java from interface to abstract class
- [Core] Changed Category.java from interface to abstract class
- [Core] Fixed category breaking on addPlugin(); Fixed configuration
- Onboard some basic checkstyle checks
- Resolve Java compiler warnings
- Miscellaneous Project Cleanup
- Hub, AmplifyOperation and Event Implementation (#12)
- Enable remaining class design checkstyles (#18)
- Enable Java 8 source features (#20)
- Require Javadocs on all public and protected software entities (#22)
- Adds AWS S3 storage plugin
- Adds UploadFile operation to AWS S3 Plugin
- Adds support for adding metadata to an upload in AWS S3 Storage Plugin
- Fixes bug with AWS S3 Storage Plugin DownloadFile
- Adds Remove Operation to S3 Storage Plugin
- Refactors Storage Operations to be specific again to handle operations without cancel/resume
- Adds List operation to S3 Storage Plugin
- Remove stale docs tree (#29)
- Use latest Gradle and Gradle Wrapper (#30)
- Resolve current checkstyle errors (#31)
- Miscellaneous code cleanup
- Refactoring of internal Hub plugin implementation details
- CircleCI initial setup (#27)
- Category/Plugin configuration for core + S3 Storage Plugin configuration (#35)
- [API] API core framework
- [API] OkHttp-based API plugin supporting GraphQL (#37)
- [API] Add ApiOperation + refactor Query (#38)
- Minor fixes
- Fix null casting error in GsonResponseFactory (#42)
- [API] Add variable support (#41)
- [API] Support other authorization modes (#43)
- [Core] Introduce Amplify Operation Request (#25)
- Cleanup callback, listener, subscriber language (#48)
- Latest-n-greatest AWS SDKs, versions 2.16.2. (#49)
- [DataStore] Add Model, ModelSchema and StorageAdapter interface (#44)
- Use a single version of the AWS SDK across all modules (#50)
- [DataStore] Add SQLiteHelper and setUp implementation (#51)
- [DataStore] Observable APIs for DataStore and LocalStorageAdapter (#52)
- Updates S3 Storage Plugin with proper escape hatch and nonblocking operations
- [DataStore] CreateSqlCommand (#59)
- [DataStore] Basic plumbing for Sync Engine (#55)
- Class and member scoping cleanup (#63)
- [Core] Generic plugin management logic in Amplify facade (#64)
- [DataStore] Support creation of database indexes through @Index annotation (#62)
- Don't use returnDefaultValues in testOptions (#65)
- Add path to store test results (#67)
- [API] Base code for building GraphQL request dynamically
- [WIP] [StorageAdapter] SetUp and Save operation (#66)
- Move Model and annotations package into amplify-core
- Fixes getContent method of GraphQLRequest
- Initial mutation doc body
- Add implementation for query (#69)
- Update API names, codegen for mutation
- Removes unused Enum interface
- Addresses checkStyle issues
- Fixes sync engine test
- Removed wrongly commited amplify config
- Fixes name of exception variable
- Adds DELETE functionality
- [StorageAdapter] Cleanup and fix race condition in setUp (#73)
- Changes primitive types to Object types
- [API] Support WebSocket-based GraphQL subscriptions (#72)
- Updates enum to no longer depend on Gson
- Miscellaneous code cleanups (#75)
- Resolves linter issues and adds back codegen test
- Add SqliteDataType enum (#76)
- Addresses PR comments
- Increase operation timeout in failing SyncEngine test
- [API] Initial predicate code supporting field equality operation (#80)
- [DataStore] Add ModelProvider; Add category & plugin infrastructure for DataStore (#77)
- [DataStore] Added connection and foreign keys (#81)
- Adds query GET operation generator code (#82)
- Updates sample model to have identical property names to schema
- [DataStore] Enable communication between StorageEngine and SyncEngine (#83)
- [DataStore] Fix inconsistencies in tests (#85)
- [API] Overrides equals and hashcode on Model (#87)
- [API] Split integration tests that use different models (#84)
- Adds correct serialization for Date properties (#89)
- Completes Predicate Structure Code (#90)
- [Tests] Separate utilities from shared models (#91)
- [TestUtils] Create a re-usable LatchedResultListener (#92)
- Codegen for Query LIST type + predicate translation (#94)
- [DataStore] Add lifecycle management and support for enums (#86)
- [Core] Wire ModelSchema for equality and collection hashing (#95)
- Raphk/sqlite bug fix (#96)
- [Model] Fail defensively on type converstion errors (#97)
- [DataStore] Publish enqueued DataStore changes via API (#99)
- Re-implement foreign-key logic (#98)
- [API] Adds support for GraphQL results with multiple items (#100)
- [DataStore] Move initialization into configure, and add e2e test (#101)
- Update analytics interface
- Adds codegen for subscriptions in the non authorized case (#105)
- Update plugin to match interface
- Adds optional mutation conditions and makes condition optional for query (#107)
- [Infrastructure] Add gradle-maven publish scripts (#104)
- Segregate SQLite related metadata from ModelSchema and ModelField (#103)
- [DataStore] Simplification to Record deserialization (#109)
- Implement update model flow (#108)
- [DataStore] Fix model to support multiple (or zero) indexes per schema (#112)
- Basic belongsTo support without fields param on connection (#111)
- Support deletion and subscription (#110)
- [DataStore] Subscribe to API changes via SyncEngine (#114)
- [DataStore] Increase timeouts in model synchronization tests (#116)
- [Test] Add Recommended config for Robolectric (#117)
- Analytics configuration and record event API
- Revert "Analytics configuration and record event API"
- GraphQLRequest generation string case fix (#119)
- [DataStore] Support target naming foreign key (#115)
- Review comments
- Fix checkstyle failures
- [DataStore] Implement eager loading foreign key on a query (#120)
- [API] Further refines GraphQLRequest Generation for Relationships (#121)
- [DataStore] Support query predicates (#122)
- [API] Adds tests/fixes for hasOne and many to many relationships (#124)
- [Testing] Updates test models to latest model codegen
- [API] Update API key used for GraphQLInstrumentationTest (#128)
- [API] Remove unused import causing checkstyle violation (#130)
- [Testing] Updates test models to latest codegen
- [API] Removes Unused Subscribe with Predicate (#131)
- [Core] Add logic to noitfy upon host availability (#125)
- [Logging] Add a simple logging plugin (#129)
- Remove AWSMobileClient initialization logic from plugin implementation (#56)
- Delete unnecessary file from last commit
- [API] Instrumentation Tests can Build and Run (#134)
- [API] Document the output of the AppSync Request Factory (#135)
- [API] Use a stable ordering for fields in GraphQL requests (#136)
- [TestModels] Organize code-generated models into directories (#133)
- [TestUtils] Simplify the Test Latch APIs (#137)
- [TestModels] Fix Broken hashCode() on Generated Models (#139)
- [DataStore] Use prepared statement for query (#126)
- [API] Allow deserialization of a List of String into an array response (#138)
- Matched configuration keys (#141)
- [DataStore] Support persisting and upgrading model version (#132)
- [Framework] Exception handling overhaul (#140)
- Initial commit for GET query in API Category
- Addressed PR changes
- Fixed the tests
- Added check style fix
- [DataStore] ModelSync Contract and Data Objects (#127)
- [DataStore] Update Code Base to Use New Code Gen Models & API (#142)
- Analytics configuration and record event api (#118)
- Support for API Key auth in REST (#146)
- [DataStore] API Interface (#149)
- Add awsconfiguration json and move amplifyconfiguration json to androidTest (#150)
- [StorageEngine] Chain upgrade models async calls using flatMap (#147)
- Update configuration structure reduce auto flush interval for tests (#151)
- [Hub] Rename plugin to AWSHubPlugin (#152)
- Added Beta tag to title
- [DataStore] Use Raw Strings Against API Category Behavior (#154)
- Overload API methods to not require API name parameter (#148)
- [DataStore] All API Interface Methods tested and working (#156)
- [DataStore] List deserialization as detail of response deserialization (#157)
- Update README.md (#155)
- [DataStore] Fixes linting error (#158)
- Update broken link on README.md (#159)
- redact credentials (#160)
- Update README.md
- Add changelog (#162)
- [DataStore] Implement delete cascade (#167)
- [Storage] Fixes config keys (#168)
- Update links in README (#161)
- Added headers for rest request (#165)
- [DataStore] Create RemoteModelState class (#166)
- [DataStore] Remove API name logic (#164)
- [DataStore] Removes condition from failing integration test (#172)
- Update artifact description to indicate preview version (#173)
- Update changelog (#174)
- Fix group id typo (#175)
- Update core artifact id (#176)
- [Core] Move Immutable helper to utils Java package (#180)
- Enable integration tests in CircleCI (#163)
- [Core] Cleanup code quality issues in Amplify facade (#181)
- Update README.md (#189)
- Update README.md (#184)
- [CircleCI] Update scripts for pulling remote configs (#183)
- Adds field for denoting owner based authorization (#188)
- Fix test for invalid foreign key (#190)
- [DataStore] Add Data Hydrator (#177)
- [DataStore] Fix import of Immutable (#192)
- [Analytics] Add support for global event properties (#153)
- [DataStore] Implement conditional save for Storage Engine (#191)
- [SQLiteStorageAdapter] Minor refactoring (#194)
- [Core] Lambdas in StreamListener, ResultListener (#197)
- [TestUtils] Recycle Synchronous Latching Code (#201)
- [Core] Tighten Type Bounds on Error Consumers (#200)
- [Storage] Split success and error callbacks (#205)
- Use Retrolambda for broader consumer compatibility (#204)
- [API] Accept Lambdas for REST Behaviours (#206)
- [API] Accept Lambdas in GraphQL Behaviors (#218)
- [DataStore] Implement in-memory predicate evaluator (#196)
- [DataStore] Pure Lambda API for DataStore (#207)
- [Publishing] Consolidate Gradle Hooks for Maven Publishing (#208)
- Restore build (two separate git commits) (#222)
- Fully remove ResultListener and StreamListener from code base (#224)
- [API] IAM support for REST api request (#178)
- Make direct use of sets (instead of iterators) in storage engine test (#226)
- [DataStore] Implement conditional delete for storage engine (#198)
- [DataStore] Refactor in-memory storage adapter (#227)
- [DataStore] Always close SQLiteCursor in instrumentation test (#228)
- [API] Fixes #187: Subscription blocking + web socket connection error not correctly reported (#217)
- Update checkstyle rules to accept variable copyright year (#231)
- Overhaul module build scripts (#235)
- Use xlarge resource class for CircleCI build
- [CircleCI] Add a warning in case AWS credentials not available
- OkHttp 4.3.0 -> 4.3.1
- Add User-Agent header to all outbound network requests
- Use latest Checkstyle version 8.28.
- Bubble up consumed failures in synchronous test utils
- Generate AmplifyConfiguration from a factory method (#239)
- [core] Add an initialize method to the Category and Plugin (#246)
- Use latest AWS SDK release 2.16.7 (#255)
- [DataStore] Use Hub notifications to communicate Sync Engine events (#251)
- [Storage] Refactor and component test (#254)
- Fix the error that caused configuration to fail unless mobile client was initialized (#257)
- Make builder methods public accessible (#258)
- Update copy-configs (#260)
- Patch flaky tests for CI/CD (#263)
- Update README.md (#261)
- Resolves some issues found by LGTM.com. (#267)
- [aws-storage-s3] Suppress autovalue warning from Robolectric (#266)
- [core] unit tests and tweaks for Category (#250)
- [aws-datastore] Synchronous adapter utility for instrumentation tests (#265)
- [core] Move NoOpCancelable into core module (#269)
- [aws-datastore] Align names of SyncEngine components to iOS, JavaScript (#272)
- [aws-datastore] Separate Sync Engine from App Sync code (#271)
- [rxbindings] Add an RxJava2 facade (#268)
- Use latest dependency versions (#275)
- Fixes to accomodate consuming Amplify from an app (#283)
- [aws-datastore] CRUD type mapping is backwards (#284)
- Use try-with-resources statement to assure that cursor is closed (#282)
- Refactor Storage operations and service (#286)
- Refactor storage options to abstract out common properties (#285)
- [Storage] Support getUrl() (#248)
- [testutils] Add synchronous storage and mobile client to testutils (#281)
- Use "master" as the default version name (#288)
- Add storage instrumentation tests (#290)
- [aws-datastore] Wait for initialization before attempting operations (#287)
- [aws-datastore] Surface more detail in exception when sync engine fails to publish
- [aws-datastore] Clarify nullability of fields in ModelSchema, SQLiteTable
- [aws-datastore] Cleanup error handling in SQLiteStorageAdapter
- [aws-datastore] System models now provided external to storage adapter
- Update contributors guide (#299)
- [Storage] Make list return Amplify, not service, key (#291)
- Don't rename source file attribute in ProGuard (#305)
- Android Gradle Plugin to 3.6.1. (#306)
- [Analytics] Support Auto session tracking initial commit (#233)
- [aws-api] @Ignore subscribeFailsWithoutProperAuth (#310)
- Use API 29 emulator for CircleCI runs
- Revert "Use API 29 emulator for CircleCI runs"
- Retain insertion order of categories in Amplify facade (#307)
- Fix path to copy-configs script (#317)
- [aws-storage-s3] Temporarily ignore upload and download tests (#318)
- [aws-datastore] Perform base sync at startup and save model versions
- [aws-datastore] Separate create and update types for storage adapter
- [aws-datastore] Don't ignore update and delete outbox tests
- [aws-datastore] Handle create update and delete in MutationProcessor
- [aws-datastore] All network data passed through merger
- Add Android device details to User-Agent
- [Analytics] Add api to attach user information to the endpoint (#312)
- [aws-datastore] Re-enable test for cloud to local synchronization
- [aws-api] Use wants to provide multiple path segments
- Remove Retrolambda
- Use the latest Gradle release, 6.2.2.
- [aws-api] Test two subscriptions for the same thing have unqiue ACKs
- Raise minSdkVersion to 16 (#327)
- Update the plugin key for analytics to match one generated by the cli and update configuration method (#328)
- Temporarily ignore broken test (#332)
- Plugin configuration requirement removed (#333)
- Advertise version 0.10.0 in the README.md.
- Fixes Date fields being converted to current date (#335)
- [Predictions] core models for interpret (#331)
- Improve WhitespaceAround checks
- Don't enforce MagicNumber check on hashCode().
- Latest Gradle and Android Gradle Plugin
- [aws-datastore] Reprovision if system models change
- Miscellaneous lint cleanups
- [aws-datastore] Persist model sync time to enable delta sync
- [aws-datastore] Mutation outbox precedence over cloud content
- Fix language code -> enum conversion
- Apply suggestion to save disk space
- Optimize!!!
- Minimize accessibility whereever possible (#329)
- [core] Miscellaneous lint cleanups and spelling fixes
- [aws-datastore] Single responsibility for SynchronousStorageAdapter
- Implement offline interpret (#336)
- [aws-api] Run tests against ApiCategory, not Amplify (#354)
- [aws-storage-s3] Test against StorageCategory not Amplify (#355)
- [aws-datastore] Test against DataStoreCategory not Amplify (#362)
- Transitive dependencies already included for AWS Mobile Client (#359)
- Use Latest SDK, 2.16.11. (#361)
- Implement interpret with AWS Comprehend (#346)
- Run tests against PredictionsCategory, not Amplify (#364)
- Add unit tests for offline predictions (#363)
- Improve feature comparison in tests (#365)
- Re-enable ignored storage test (#367)
- [aws-analytics-pinpoint] Introduce builder for AnalyticsEvent (#353)
- Implement online translate using AWS Translate (#369)
- [core] Handle missing configuration file (#376)
- [testutils] Prevent deadlocks in Latch and Await (#375)
- Enforce a consistent annotation specification style (#372)
- Basic cleanups in the README.md (#381)
- [aws-datastore] Move AppSync type enumerations out of core (#385)
- [aws-datastore] Maintain insertion order in ModelProviders (#389)
- [aws-datastore] Log inbound merge events as information (#390)
- Use File instead of String input parameters (#391)
- [aws-api] Use latest OkHttp3, 4.5.0. (#387)
- Android Gradle Plugin to 3.6.3 (#388)
- [aws-datastore] AWSDataStorePlugin via Constructor (#397)
- Expose all GraphQL error properties (#392)
- Implement online celebrity + label detection (#394)
- Expose all GraphQL error properties (part 2) (#400)
- Add MissingJavadocType to checkstyle-rules (#403)
- Add Javadoc to resolve checkstyle error (#404)
- Temporarily disable failing REST API test (#411)
- Cleanups to README, CHANGELOG, CONTRIBUTING (#413)
- Implement online entity detection (#399)
- A few small doc tweaks to README.md, CONTRIBUTING.md. (#415)
- [aws-api] Basic GraphQL components tests for AWSApiPlugin (#414)
- [aws-datstore] Implementation of user-provided DataStore configs (#401)
- fix datastore javadoc checkstyle (#418)
- add support for non-model types in DataStore (#398)
- Fix JsonParseException when parsing null path in Error object (#412)
- Flip constant equality checks (#420)
- [aws-datastore] Rename Mutation as SubscriptionEvent (#419)
- Miscellaneous changes for Predictions (#421)
- [aws-api] Handle AppSync date time scalar types (#407)
- Add unit test for result transfomer utilities (#422)
- Format JSON so it is more readable (#423)
- Implement online text detection  (#409)
- [aws-api] Reinstate List, Separate Date and AWSTimestamp
- [aws-datastore] Multidex support for API levels 16-21
- [aws-storage-s3] Ignore flaky resume test
- [aws-datastore] Ignore MutationProcessorTest#canDrainMutationBox (#429)
- [core] Ignore HubInstrumentedTest#multiplePublications (#430)
- Do not require a particular profile to run copy-configs (#428)
- [aws-datastore] Only start Orchestrator if API configured (#425)
- [aws-datastore] Decompose StorageItemChange into smaller pieces
- [aws-datastore] Emit StorageItemChange from LocalStorageAdapter
- [aws-datastore] Spilt StorageItemChange and PendingMutation
- Add unit tests for predictions result transformers (#426)
- [aws-datastore] Move model concerns into model subpackage (#435)
- [core] Remove a superfluous utility class (#436)
- Update CONTRIBUTING.md
- [core] Remove some sparesely or unused constructs (#440)
- [aws-datastore] PendingMutations are now Comparable according to their creation times (#439)
- [DataStore] query API changes with pagination support (#438)
- Adds some miscellaneous, missing method documentation. (#437)
- [core] Break apart utils classes (#442)
- [aws-datastore] Test reliability in TimeBasedUuidTest (#446)
- [aws-datastore] Logging cleanups (#449)
- [aws-datastore] Clarify AWSDataStorePlugin dependency on API Category (#450)
- Simplify Hub and HubAccumulator (#448)
- Implement online text to speech conversion (#432)
- [DataStore] fix type conversion for enums and dates (#447)
- Auth category (#445)
- [aws-datastore] thread safe rx publish subjects (#454)
- Add Discord badge (#456)
- Update Auth Session to Final Design (#457)
- [aws-datastore] Refactor SubscriptionProcessor (#467)
- [aws-datastore] Fix unhandled exceptions when no API is available (#459)
- Adds Hub events for Auth (#462)
- Adds sign out to Auth (#463)
- [aws-auth-cognito] Fix checkstyle issue (#471)
- [aws-datastore] Include mutation details in publication exception (#473)
- [aws-datastore] Remove an unused constant (#474)
- [aws-datastore] Additional logging (#476)
- Allow a global log level in the AndroidLoggingPlugin (#475)
- [aws-analytics-pinpoint] Minor formatting and style tweaks (#477)
- [aws-datastore] Further refinements to log levels (#478)
- [aws-datastore] Minor code style rweaks in SQLiteStorageAdapter
- [aws-datastore] three-way merge logic for unconditional mutations (#460)
- [aws-datastore] Optimize record lookup (#480)
- Use latest dependency versions (#482)
- Add text and label configuration (#481)
- [core] Repackage REST and GraphQL Behaviors interfaces (#485)
- Adds signInWithWebUI and signInWithSocialWebUI (#486)
- [aws-datastore] Permit multiple mutations per model ID (#491)
- [aws-datastore] Remove mutation before merge in MutationProcessor (#492)
- [aws-datastore] Mark mutations as in-flight while processing (#495)
- [aws-datastore] Serialize observation of storage (#493)
- [aws-datastore] Consider the model version before merging (#494)
- [testutils] Remove logically incorrect HubAccumulator test (#500)
- Auth Integration for Storage (#497)
- Auth Integration for API (#501)
- Auth integration with Analytics (#503)
- Integrates Auth with Predictions, Updates Sign Out, Disables AWS Logs (#506)
- [aws-datastore] Improve list-scattering routines in test coe (#507)
- [aws-api] add pagination (#488)
- [aws-datastore] Clarify field naming in PersistentRecord (#508)
- (feat) amplify-tools as a separate project (#505)
- [core] Add equals hashCode toString for API data classes (#511)
- [core] Add equals hashCode toString for Analytics data types (#510)
- Fixes global signout exception handling (#512)
- Add AmplifyDisposables utility to each modules using Rx (#509)
- [aws-datastore] Implement DataStore clear feature (#416)
- Subscription authorizer for API (#502)
- [aws-datastore] adding predicate parameter to the necessary APIs (#496)
- [aws-datastore] Handle list responses without any items (#515)
- Return empty array instead of null for empty items in response (#516)
- Add equals, hashcode to VariablesSerializers (#517)
- Rename AWS* to Temporal* in the TemporalDeserializers (#518)
- [aws-datastore] equals, toString, hashCode for config and conflict handler (#519)
- Update OkHttp to latest, 4.7.2. (#520)
- [aws-api] Move mockwebserver to the test dependencies section (#521)
- Android Studio Reformat (⌥⌘-L)
- Fix IDE integrations for Windows
- Fix modelgen
- Fix amplifyPush
- Set VERSION_NAME to master
- [aws-api] Fix POST requests with non-empty body (#525)
- Remove AWSCredentialsProvider behavior (#528)
- Removes allowAccessModification from ProGuard (#529)
- README Updates for 1.0.0 (#523)
- Show build outputs and unit test results in CircleCI Web UI (#534)
- Android Gradle Plugin 4.0.0, Gradle 6.4.1, AndroidX Core 1.3.0.
- Add time constraints on unbounded blocking calls (#535)
- Fix Getting Started link (#569)
- ci: Try build and integrationtest 3 times before failing (#573)
- ci: improve integration test retry logic (#608)
- vend codecov.io bash script ourselves instead of downloading (#609)
- [aws-datastore] fix model name plural form creation in AppSyncRequestFactory (#628)
- GraphQLRequestFactory refactor & add owner based auth support to API category (#596)
- feat(datastore) Add support for owner based auth (#635)
- fix(datastore) add deserializers for dates (#642)
- fix(datastore) Race condition fix and other stability-related fixes (#599)
- fix(datastore) Missed a function name change during merge (#643)
- fix(api) Fix tests broken by PR #599 (#647)
- fix(datastore) Set isOrchestratorReady flag to true  when no API is configured (#653)
- chore(datastore) Remove all API deserialization logic in DataStore, and rely on API instead (#665)
- fix(amplify-tools) added a null check for RunManagerNode by parsing of workspace.xml (#673)
- Revert "chore: enable coverage reports (#592)" (#677)
- Sends proper user agent for Auth (#661)
- Fixes unit test for new Auth User Agent (#694)
- Fix broken assert statement (#705)
- Updates to latest SDK version (#708)
- feat(datastore) Adding SyncType to LastSyncMetadata (#713)
- feature(core): allow additional platforms in amplify config (#703)
- increase jvm max heap size (#717)
- Fix (datastore) quote sql commands (#712)
- feature: adds progress callbacks to storage (#680)
- feat(api) Implement Comparable for Temporal types so they can be used in predicates (#721)
- feature(datastore) Trigger network-related hub events (#716)
- Trying to pin the image version (#733)
- Pull version name from BuildConfig (#735)
- Revert #734 (#736)
- fix(datastore) quote drop table command (#728)
- feat(api) PaginatedResult now implements Iterable (#750)
- chore(ci) ignore tests failing due to resources that no longer exist (#754)
- Resolve a typo in #Contributing via Pull Requests (#756)
- feat(datastore) add pagination, respecting syncPageSize and syncMaxRecords (#737)
- feat(DataStore) add sorting (#633)
- chore(spelling) Duplicate the (#761)
- chore(core) Enable logging during unit tests (#744)
- feat(rxbindings) RxBindings improvements. (#771)
- feat(datastore) Trigger hub events during sync (#710)
- fix(rxbindings) Make ConnectionStateEvent public (#774)
- show toast when issue body is copied (#775)
- Revert "fix(aws-datastore): selection set for nested custom types (#730)" (#777)
- Categorize auth errors (#770)
- ci: improve integration test retry logic
- Increase timeout for AppSyncClientInstrumentationTest
- Amplify release version 1.3.1 (#797)
- fix(datastore) Add missing converters for AWSTimestamp used by SQLiteStorageAdapter (#783)
- Revert "Revert "fix(aws-datastore): selection set for nested custom types (#730)" (#777)" (#801)
- fix(api) owner with readonly access should be able to sub to on_delete, on_create, on_update (#807)
- feature: user can provide a datastore config (#810)
- Fix configuration of mavenLocal in 'Consuming Development Versions' (#819)
- Fix a typo (#820)
- chore(build) Update awsSdkVersion dependency to 2.19.0 (#828)
- fix(api) Fix ClassCastException when building selection set for custom type (#829)
- fix(api) Throw exception when constructing Date from invalid input String (#825)
- chore(datastore) Represent lastChangedAt as Temporal.Timestamp instead of long (#670)
- chore(aws-api-appsync) Use class as value instead of simpleName for JavaFieldType in case of duplicated Class simple name. (#839)
- Adds support for mixed owner and group based auth rules (#860)
- chore(datastore) Remove unused AWS_GRAPH_QL_TO_JAVA map (#866)
- chore(devmenu) ignore failing instrumentation tests (#865)
- chore(api) remove JAVA_DATE since codegen does not generate java.lang.Date (#867)
- Add stale configuration (#878)
- use the latest release in README (#882)
- (fix) Fail early and throw useful exception if customer forgets to call Amplify.configure (#888)
- feature(aws-datastore): handle mutation conflicts (#883)
- fix(datastore):Prevent concurrent start/stop on orchestrator (#876)
- Enable setting of the server side encryption algorithm in StorageUploadFileOptions (#886)
- feature: resolve conflicts according to strategies (#904)
- Update README to reference latest release 1.4.2 (#915)
- feature(aws-api): support custom group claim (#930)
- feat(datastore) Add start and stop, and stop starting on configure and clear (#909)
- checkstyle fixup (#941)
- feat(datastore) swallow unauthorized errors for subscriptions (#942)
- feature(build): Trigger unit and integration tests in CodeBuild (#927)
- fix(datastore) only fire DataStore Hub READY event if not already started (#952)
- Add Builder for AuthRule (#938)
- recursively build joins for multilevel nested models (#892)
- feature(aws-datastore): support for hybrid platforms (#954)
- Add upload InputStream API (#955)
- feat(datastore) selective sync (#959)
- chore(release) 1.6.2 (#974)
- chore(rxbindings) second attempt to fix transient test failures (#990)
- chore(predictions) fix integ tests (#992)
- fix(datastore) release startStopSemaphore when start returns, not when API sync completes (#1027)
- [aws-api] Fix DELETE calls not working with v4 signer (#1037)
- feature: parallelize integ test config downloads (#1042)
- fix(datastore) query results should be sorted when sort order provided (#1049)
- Send unix epoch in OutboxMutationEvent instead of Temporal.Timestamp. (#1052)
- MutationProcessor - Fix missing schema on SerializedModel mutations. (#1051)
- Update README.md (#1044)
- feat(api) response deserialization should only skip top level for specific response types (#1062)
- chore(api) remove responseType from GraphQLOperation, since we already know it from the GraphQLRequest (#1063)
- Release 1.6.8 (#1065)
- fix(datastore) publish networkStatus event at correct times (#1067)
- chore(datastore) missing column name should be a verbose log since it is expected for relationship fields (#1068)
- chore(datastore) verbose log instead of warn when deleting a non existent item (#1081)
- fix(datastore) Defer merger.merge to avoid failure if outbox has mutation (#1082)
- fix(datastore) Make PersistentMutationOutbox operations synchronized (#1085)
- fix(api) serialize nulls on requests to support setting fields to null (#1091)
- chore(datastore) remove overloaded query method in favor of just one (#1092)
- chore(release) 1.6.9 (#1097)
- refactor sqlite storage adapter (#1093)
- Throw AlreadyConfiguredException on reconfiguration attempt (#1109)
- datastore(feat): support delete by model type with predicate (#1106)
- chore(devmenu) use java.util.Date instead of java.time for dev menu logging (#1117)
- feat(datastore) only include changed fields in update mutations (#1110)
- chore(core) increment SDK version to 2.22.0 (#1118)
- fix(datastore) fix crash caused by null patchItem (#1123)
- chore(datastore) minor simplification in SQLiteStorageAdapter (#1120)
- Make MatchAll/NoneQueryPredicate classes private (#1127)
- Release 1.6.10 (#1124)
- fix incorrectly serialized model for delete (#1131)
- chore(ci/cd):Generate build reports from DF data (#1136)
- feat(datastore) add support for notContains query operator (#1145)
- fix(datastore) include cause on error thrown when observing storage times out (#1165)
- chore(devmenu) set dev menu disabled by default (#1167)
- chore(ci) retry build 3 times before failing (#1166)
- release: 1.16.14 (#1198)
- ci: fix maven publishing (#1200)
- release: 1.16.15 (#1201)
- release: Core kotlin 0.1.1 (#1202)
- release: 1.17.0 (#1213)
- fix(rest-api) Expose HTTP headers in RestResponse (#1184)
- fix(datastore) improve storage adapter performance (#1161)
- chore(datastore) ignore OperationDisabled errors in SubscriptionProcessor (#1209)
- chore(release) Release v1.17.1 (#1239)
- feat(api):allows callers to specify auth mode (#1238)
- fix(datastore) Make PendingMutationConverter work for SerializedModel (#1253)
- release: Amplify Android 1.17.3 (#1285)
- depend on latest sdk version (#1294)
- Upgrade dependency on the latest SDK version (#1296)
- release: Amplify Android 1.17.4 (#1299)
- release: Amplify Android 1.17.5 (#1305)
- release: Amplify Android 1.17.6 (#1336)
- create stale bot GitHub action (#1337)
- refactor:add enum to represent auth rule provider (#1320)
- default to mobile client for iam auth mode (#1351)
- release: Amplify Android 1.17.7 (#1354)
- release: Amplify Android 1.17.8 (#1367)
- release: Amplify Android 1.18.0 (#1372)
- chore(api):tweaks to the api init process (#1309)
- Update stale.yml (#1380)
- release: Amplify Android 1.19.0 (#1382)
- Update SDK version to 2.26.0 (#1386)
- release: Amplify Android 1.20.0 (#1387)
- release: Amplify Android 1.20.1 (#1400)
- release: Amplify Android 1.21.0 (#1406)
- Update stale.yml (#1404)
- chore:fix dependabot alert for addressable gem (#1410)
- release: Amplify Android 1.22.0 (#1418)
- Delete stale.yml (#1421)
- Updated DataStore delete test based on expected delete behavior (#1423)
- feat(api) add CUSTOM case to AuthStrategy (#1428)
- release: Amplify Android 1.23.0 (#1433)
- release: Amplify Android 1.24.0 (#1445)
- release: Amplify Android 1.24.1 (#1457)
- release: Amplify Android 1.25.0 (#1467)
- fix(predictions):remove invalid test (#1476)
- release: Amplify Android 1.25.1 (#1477)
- release: Amplify Android 1.26.0 (#1492)
- release: Amplify Android 1.27.0 (#1508)
- Fix for issue with foreign keys on schema upgrade delete (#1501)
- better announce which schema is failing to sync (#1479)
- Observe query (#1470)
- release: Amplify Android 1.28.0 (#1517)
- Observe query updates (#1520)
- Update AWS SDK ver to 2.33.0 (#1526)
- release: Amplify Android 1.28.1 (#1528)
- fix(datastore):predicate handling for observe (#1537)
- release: Amplify Android 1.28.2 (#1539)
- Update build.gradle (#1553)
- release: Amplify Android 1.28.3 (#1560)
- release: Amplify Android 1.29.0 (#1569)
- Update build.gradle (#1578)

[See all changes between 0.12.3-rc and 0.13.0](https://github.com/aws-amplify/amplify-android/compare/release-kotlin_v0.12.3-rc...release-kotlin_v0.13.0)

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
