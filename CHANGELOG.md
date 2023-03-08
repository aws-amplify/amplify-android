## [Release 1.39.0](https://github.com/aws-amplify/amplify-android/releases/tag/release_v1.39.0)

### Features
- **rxbindings:** add binding for predictions ([#548](https://github.com/aws-amplify/amplify-android/issues/548))
- **API:** Remove nextToken from ModelPagination since nextToken is never exposed ([#557](https://github.com/aws-amplify/amplify-android/issues/557))
- **API:** Add AuthRule annotation to support @auth directive ([#559](https://github.com/aws-amplify/amplify-android/issues/559))
- **rxbindings:** add bindings for auth ([#579](https://github.com/aws-amplify/amplify-android/issues/579))
- **auth:** Adds user info to sign up result ([#732](https://github.com/aws-amplify/amplify-android/issues/732))
- **auth:** Adds improved HostedUI functionality ([#739](https://github.com/aws-amplify/amplify-android/issues/739))
- **aws-auth-cognito:** implement device tracking ([#738](https://github.com/aws-amplify/amplify-android/issues/738))
- **devmenu:** Add developer menu
- **aws-datastore:** implement mutation outbox hub events ([#762](https://github.com/aws-amplify/amplify-android/issues/762))
- **core:** filter developer menu logs by log level ([#788](https://github.com/aws-amplify/amplify-android/issues/788))
- **core:** Adds option to disable dev menu ([#821](https://github.com/aws-amplify/amplify-android/issues/821))
- **Auth:** Add user attribute operations (Fetch/Update/Verify/Confirm user attributes) ([#798](https://github.com/aws-amplify/amplify-android/issues/798))
- **aws-api:** support owner-based auth with oidc ([#906](https://github.com/aws-amplify/amplify-android/issues/906))
- **Auth:** Adds the ability to specify a custom browser for web UI sign in/out ([#1030](https://github.com/aws-amplify/amplify-android/issues/1030))
- **aws-datastore:** add mutation failed hub event ([#976](https://github.com/aws-amplify/amplify-android/issues/976))
- **datastore:** support sorting by related models ([#1141](https://github.com/aws-amplify/amplify-android/issues/1141))
- a kotlin-centric facade for amplify android
- **aws-api:** Support Custom OkHttp Configurations ([#1207](https://github.com/aws-amplify/amplify-android/issues/1207))
- **aws-auth-cognito:** Allows userattributes in confirmSignIn ([#1343](https://github.com/aws-amplify/amplify-android/issues/1343))
- add supporting types for multi-auth ([#1346](https://github.com/aws-amplify/amplify-android/issues/1346))
- **datastore:** adding multiauth support ([#1347](https://github.com/aws-amplify/amplify-android/issues/1347))
- **datastore:** Multiauth integration tests ([#1349](https://github.com/aws-amplify/amplify-android/issues/1349))
- **datastore:** Return nested data for belongsTo associations in datastore ([#1390](https://github.com/aws-amplify/amplify-android/issues/1390))
- **aws-auth-cognito:** Adds clientMetadata to AWSCognitoAuthSignUpOptions ([#1407](https://github.com/aws-amplify/amplify-android/issues/1407))
- add support for AWS_LAMBDA auth type ([#1412](https://github.com/aws-amplify/amplify-android/issues/1412))
- **auth:** add options to resendSignUpCode ([#1422](https://github.com/aws-amplify/amplify-android/issues/1422))
- **auth:** add options to resetPassword and confirmResetPassword ([#1426](https://github.com/aws-amplify/amplify-android/issues/1426))
- **datastore:** Retry sync ([#1414](https://github.com/aws-amplify/amplify-android/issues/1414))
- **datastore:** Add non-model type support for amplify-flutter ([#1459](https://github.com/aws-amplify/amplify-android/issues/1459))
- **geo:** Added support for Geo category ([#1502](https://github.com/aws-amplify/amplify-android/issues/1502))
- **datastore:** Add support for parsing match none predicate in AppSync request builder ([#1515](https://github.com/aws-amplify/amplify-android/issues/1515))
- **geo:** Add search APIs ([#1558](https://github.com/aws-amplify/amplify-android/issues/1558))
- **api:** Add support for custom GraphQL endpoints. ([#1564](https://github.com/aws-amplify/amplify-android/issues/1564))
- **maplibre-adapter:** MapLibreView and Geo adapter integration ([#1568](https://github.com/aws-amplify/amplify-android/issues/1568))
- **maplibre-adapter:** amplify map view with markers and search capabilities ([#1598](https://github.com/aws-amplify/amplify-android/issues/1598))
- Add support for custom prefix resolver ([#1659](https://github.com/aws-amplify/amplify-android/issues/1659))
- **auth:** Add deleteUser API ([#1656](https://github.com/aws-amplify/amplify-android/issues/1656))
- Add authFlow type in signInOptions ([#1686](https://github.com/aws-amplify/amplify-android/issues/1686))
- **maplibre-adapter:** add clustering to map view ([#1692](https://github.com/aws-amplify/amplify-android/issues/1692))
- initial dev-preview commit to migrate to Kotlin SDK ([#1712](https://github.com/aws-amplify/amplify-android/issues/1712))
- Add flushEvents HubEvent for analytics ([#1792](https://github.com/aws-amplify/amplify-android/issues/1792))
- **auth:** Delete user implementation ([#1767](https://github.com/aws-amplify/amplify-android/issues/1767))
- Initial changes for pinpoint migration ([#1810](https://github.com/aws-amplify/amplify-android/issues/1810))
- **auth:** Fetch devices implementation ([#1808](https://github.com/aws-amplify/amplify-android/issues/1808))
- **predictions:** migrate predictions category to use Kotlin SDK ([#1819](https://github.com/aws-amplify/amplify-android/issues/1819))
- **auth:** Remember and forget device implementation ([#1811](https://github.com/aws-amplify/amplify-android/issues/1811))
- **auth:** add reset password ([#1835](https://github.com/aws-amplify/amplify-android/issues/1835))
- **auth:** add confirm reset password use case ([#1848](https://github.com/aws-amplify/amplify-android/issues/1848))
- **pinpoint:** Targeting half of Analytics migration ([#1846](https://github.com/aws-amplify/amplify-android/issues/1846))
- **auth:** resend signup code ([#1816](https://github.com/aws-amplify/amplify-android/issues/1816))
- Add EventSubmitter to Pinpoint ([#1888](https://github.com/aws-amplify/amplify-android/issues/1888))
- **auth:** add confirm sign in with challenges ([#1859](https://github.com/aws-amplify/amplify-android/issues/1859))
- Address pending analytics todos ([#1911](https://github.com/aws-amplify/amplify-android/issues/1911))
- **predictions:** add polly presigner ([#1921](https://github.com/aws-amplify/amplify-android/issues/1921))
- **Auth:** add custom auth feature ([#1903](https://github.com/aws-amplify/amplify-android/issues/1903))
- **auth:** publish hub events for auth ([#1941](https://github.com/aws-amplify/amplify-android/issues/1941))
- **auth:** add advanced security feature data collectors ([#1928](https://github.com/aws-amplify/amplify-android/issues/1928))
- **Auth:** HostedUI Start / Signout ([#1909](https://github.com/aws-amplify/amplify-android/issues/1909))
- **Auth:** add custom endpoint ([#1949](https://github.com/aws-amplify/amplify-android/issues/1949))
- **auth:** add session expiry hub event and changes to sign in and s… ([#1951](https://github.com/aws-amplify/amplify-android/issues/1951))
- **Auth:** Implementation of devicesrp ([#1946](https://github.com/aws-amplify/amplify-android/issues/1946))
- **auth:** force refresh auth session ([#1917](https://github.com/aws-amplify/amplify-android/issues/1917))
- **core:** set user-agent system properties ([#1970](https://github.com/aws-amplify/amplify-android/issues/1970))
- **Auth:** add custom endpoint ([#1957](https://github.com/aws-amplify/amplify-android/issues/1957))
- **auth:** add device metadata to signed in data and credential store ([#1971](https://github.com/aws-amplify/amplify-android/issues/1971))
- **auth:** switch auth flow type ([#1967](https://github.com/aws-amplify/amplify-android/issues/1967))
- **auth:** Save and migrate sign in method ([#1978](https://github.com/aws-amplify/amplify-android/issues/1978))
- **auth:** Federate to Identity Provider ([#1986](https://github.com/aws-amplify/amplify-android/issues/1986))
- **auth:** create a new aws credential type and return that from apis ([#1988](https://github.com/aws-amplify/amplify-android/issues/1988))
- **auth:** Refresh hosted ui user pool token ([#1995](https://github.com/aws-amplify/amplify-android/issues/1995))
- **auth:** add user migration auth ([#1975](https://github.com/aws-amplify/amplify-android/issues/1975))
- **auth:** Auth exception updates ([#2001](https://github.com/aws-amplify/amplify-android/issues/2001))
- Add support for querying transfer by transferId ([#2003](https://github.com/aws-amplify/amplify-android/issues/2003))
- Add kotlin and RxJava facade for getTransfer API ([#2032](https://github.com/aws-amplify/amplify-android/issues/2032))
- **test:** add sign in api test generator ([#1979](https://github.com/aws-amplify/amplify-android/issues/1979))
- **auth:** audit of missing auth options ([#2090](https://github.com/aws-amplify/amplify-android/issues/2090))
- **Geo:** Add Kotlin Geo Facade ([#2155](https://github.com/aws-amplify/amplify-android/issues/2155))
- **Auth:** Overriding sign in when the State machine is already in the signing in state ([#2187](https://github.com/aws-amplify/amplify-android/issues/2187))
- **auth:** add required hash param to cognito api calls ([#2266](https://github.com/aws-amplify/amplify-android/issues/2266))
- **datastore:** Add recoverability improvements ([#2201](https://github.com/aws-amplify/amplify-android/issues/2201))
- **auth:** Added parity test for fetchDevices,rememberDevice,forgetDevice and fetchUserAttributes ([#2174](https://github.com/aws-amplify/amplify-android/issues/2174))
- **auth:** added kover plugin for coverage ([#2267](https://github.com/aws-amplify/amplify-android/issues/2267))
- **storage:** Add support for S3 acceleration mode ([#2304](https://github.com/aws-amplify/amplify-android/issues/2304))
- **aws-datastore:** Make the reachability component configurable ([#2307](https://github.com/aws-amplify/amplify-android/issues/2307))

### Bug Fixes
- **amplify-tools:** Don't escape JSON on non-Windows platforms ([#526](https://github.com/aws-amplify/amplify-android/issues/526))
- **api:** Fix [#538](https://github.com/aws-amplify/amplify-android/issues/538) by providing a default empty byte array for REST payload ([#539](https://github.com/aws-amplify/amplify-android/issues/539))
- **aws-api:** cleanups to subscription endpoint ([#542](https://github.com/aws-amplify/amplify-android/issues/542))
- **core:** use sentinel for null query predicate ([#540](https://github.com/aws-amplify/amplify-android/issues/540))
- dont run proguard only publish lib rules ([#546](https://github.com/aws-amplify/amplify-android/issues/546))
- **Auth:** Adds invalidateTokens param to properly signout HostedUI ([#570](https://github.com/aws-amplify/amplify-android/issues/570))
- **amplify-tools:** Add task to create javadoc jar ([#530](https://github.com/aws-amplify/amplify-android/issues/530))
- **DataStore:** Fix for contains predicate ([#584](https://github.com/aws-amplify/amplify-android/issues/584))
- create aws-appsync module, fix Datastore serialization bug (part 1) ([#601](https://github.com/aws-amplify/amplify-android/issues/601))
- **aws-api:** Fix path with leading slash breaking REST ([#600](https://github.com/aws-amplify/amplify-android/issues/600))
- **core:** Updated amazonaws.amplify package name to amplifyframework ([#616](https://github.com/aws-amplify/amplify-android/issues/616))
- **DataStore:** remove unnecessary observeOn call ([#612](https://github.com/aws-amplify/amplify-android/issues/612))
- update default repo version name ([#641](https://github.com/aws-amplify/amplify-android/issues/641))
- **api:** now correctly signs requests with query string params ([#658](https://github.com/aws-amplify/amplify-android/issues/658))
- daemon style lifecycle for orchestrator
- **aws-datastore:** address issue where primary key was not properly quoted ([#720](https://github.com/aws-amplify/amplify-android/issues/720))
- **aws-datastore:** remove insert statement cache ([#718](https://github.com/aws-amplify/amplify-android/issues/718))
- **datastore:** typo in sql keyword between ([#729](https://github.com/aws-amplify/amplify-android/issues/729))
- **auth:** Updates to use fixed HostedUI ([#734](https://github.com/aws-amplify/amplify-android/issues/734))
- **DataStore:** use a LinkedList and HashMap to improve the time comp… ([#613](https://github.com/aws-amplify/amplify-android/issues/613))
- **datastore:** order hydration tasks topologically ([#741](https://github.com/aws-amplify/amplify-android/issues/741))
- relax condition of translate assertion ([#740](https://github.com/aws-amplify/amplify-android/issues/740))
- do not allow changing plugins after configure ([#751](https://github.com/aws-amplify/amplify-android/issues/751))
- **aws-datastore:** selection set for nested custom types ([#730](https://github.com/aws-amplify/amplify-android/issues/730))
- rxbindings readme v2 to v3 ([#789](https://github.com/aws-amplify/amplify-android/issues/789))
- **core:** remove dev menu app icon
- **aws-datastore:** emit outbox status event on mutation process ([#790](https://github.com/aws-amplify/amplify-android/issues/790))
- **API:** Fixes owner auth subscribe with email/phone sign-in ([#818](https://github.com/aws-amplify/amplify-android/issues/818))
- **Auth:** Fixes guest / anonymous user access ([#850](https://github.com/aws-amplify/amplify-android/issues/850))
- **aws-datastore:** make subscription timeout model count dependent ([#858](https://github.com/aws-amplify/amplify-android/issues/858))
- **API:** Adds support for custom identity claim ([#889](https://github.com/aws-amplify/amplify-android/issues/889))
- align datastore config builder methods to ios ([#907](https://github.com/aws-amplify/amplify-android/issues/907))
- **api:** Add owner field to selection set if implicitly defined ([#926](https://github.com/aws-amplify/amplify-android/issues/926))
- consider deletion predicate when requested ([#949](https://github.com/aws-amplify/amplify-android/issues/949))
- **aws-datastore:** checkstyle and integration tests fix ([#957](https://github.com/aws-amplify/amplify-android/issues/957))
- integ test instructions in contributing guide
- properly support flutter online scenarios ([#961](https://github.com/aws-amplify/amplify-android/issues/961))
- **aws-predictions:** expose label instances' bounding boxes ([#968](https://github.com/aws-amplify/amplify-android/issues/968))
- **aws-datastore:** kludge to support flutter connected models ([#985](https://github.com/aws-amplify/amplify-android/issues/985))
- associated Flutter models can sync to cloud ([#988](https://github.com/aws-amplify/amplify-android/issues/988))
- **aws-datastore:** keep observations active despite errors ([#993](https://github.com/aws-amplify/amplify-android/issues/993))
- **aws-datastore:** Prevent NPE while inspecting GraphQL errors ([#1000](https://github.com/aws-amplify/amplify-android/issues/1000))
- honor deletions from AppSync when used by Flutter ([#1004](https://github.com/aws-amplify/amplify-android/issues/1004))
- **aws-datastore:** support temporal types in Flutter ([#1001](https://github.com/aws-amplify/amplify-android/issues/1001))
- fix field label in toString() of ModelField ([#1006](https://github.com/aws-amplify/amplify-android/issues/1006))
- comprehend deserializtion of primitive types ([#1007](https://github.com/aws-amplify/amplify-android/issues/1007))
- **aws-datastore:** idempotent deletions from merger ([#1014](https://github.com/aws-amplify/amplify-android/issues/1014))
- **aws-datastore:** null modelName in OutboxMutationProcessed ([#1021](https://github.com/aws-amplify/amplify-android/issues/1021))
- **aws-datastore:** Add owner to SelectionSet for Flutter ([#1020](https://github.com/aws-amplify/amplify-android/issues/1020))
- **amplify-tools:** Add --forcePush to init ([#1025](https://github.com/aws-amplify/amplify-android/issues/1025))
- **aws-datastore:** deleting nonexistent model instance no longer fails ([#1033](https://github.com/aws-amplify/amplify-android/issues/1033))
- **aws-datastore:** conditional insert no longer succeeds ([#1035](https://github.com/aws-amplify/amplify-android/issues/1035))
- **aws-datastore:** store schema float as a java double ([#1040](https://github.com/aws-amplify/amplify-android/issues/1040))
- **aws-datastore:** Resolves crash when going offline ([#1039](https://github.com/aws-amplify/amplify-android/issues/1039))
- **aws-datastore:** ignore foreign key error during sync ([#1058](https://github.com/aws-amplify/amplify-android/issues/1058))
- **aws-api:** dont fire error if rest op canceled ([#1072](https://github.com/aws-amplify/amplify-android/issues/1072))
- continue processing outbox items after removal ([#1084](https://github.com/aws-amplify/amplify-android/issues/1084))
- **aws-datastore:** publish each cascading delete ([#1059](https://github.com/aws-amplify/amplify-android/issues/1059))
- export proguard rules for consumers ([#1103](https://github.com/aws-amplify/amplify-android/issues/1103))
- **datastore:** dont use where.id() function ([#1130](https://github.com/aws-amplify/amplify-android/issues/1130))
- **core:** query field now stores model name ([#1133](https://github.com/aws-amplify/amplify-android/issues/1133))
- remove MultiDexApplication from DataStore manifest
- add a small initial delay to failing rxbinding test
- **core:** Improve error messaging when category configuration or initialization fails ([#1154](https://github.com/aws-amplify/amplify-android/issues/1154))
- Delete relationships fix for flutter ([#1168](https://github.com/aws-amplify/amplify-android/issues/1168))
- **datastore:** save root instance instead of creating multiple iterators ([#1171](https://github.com/aws-amplify/amplify-android/issues/1171))
- **rxbindings:** don't emit errors to disposed subscriber
- **datastore:** fix crash when deleting model with many children ([#1121](https://github.com/aws-amplify/amplify-android/issues/1121))
- **datastore:** ignore sqlite exceptions during cascading delete ([#1149](https://github.com/aws-amplify/amplify-android/issues/1149))
- Add support for readonly fields such as createdAt, updatedAt ([#1249](https://github.com/aws-amplify/amplify-android/issues/1249))
- **aws-api:** use default cognito provider if needed. ([#1254](https://github.com/aws-amplify/amplify-android/issues/1254))
- **datastore:** optimize sync queries with predicates by wrapping in an AND group ([#1225](https://github.com/aws-amplify/amplify-android/issues/1225))
- **kotlin:** getCurrentUser return type should be nullable ([#1265](https://github.com/aws-amplify/amplify-android/issues/1265))
- **ci:** update branch name and PR subject ([#1276](https://github.com/aws-amplify/amplify-android/issues/1276))
- change protocol for github import ([#1284](https://github.com/aws-amplify/amplify-android/issues/1284))
- throws AlreadyConfiguredException when configured == true ([#1274](https://github.com/aws-amplify/amplify-android/issues/1274))
- Use ObjectsCompat since Objects is API 19+ ([#1289](https://github.com/aws-amplify/amplify-android/issues/1289))
- adds ConfirmSignInOptions for confirmSignIn API method ([#1297](https://github.com/aws-amplify/amplify-android/issues/1297))
- signed in hub event is now fired after currentUser is set, instead of before ([#1300](https://github.com/aws-amplify/amplify-android/issues/1300))
- **datastore,api:** Update and delete mutations now work when custom primary key is defined ([#1292](https://github.com/aws-amplify/amplify-android/issues/1292))
- checkstyle failure on Windows ([#1326](https://github.com/aws-amplify/amplify-android/issues/1326))
- **datastore:** save metadata when merging even if mutation outbox has pending item ([#1319](https://github.com/aws-amplify/amplify-android/issues/1319))
- **datastore:** add syncExpression method to configuration builder that takes the modelName as a String ([#1330](https://github.com/aws-amplify/amplify-android/issues/1330))
- **api:** check for null ModelSchema to prevent crash in SerializedModel toString method ([#1341](https://github.com/aws-amplify/amplify-android/issues/1341))
- **Auth:** prevent multiple invocations of success callback for updateUserAttributes ([#1339](https://github.com/aws-amplify/amplify-android/issues/1339))
- **auth:** Add ConfirmSignUpOptions for confirmSignUp API method ([#1357](https://github.com/aws-amplify/amplify-android/issues/1357))
- **storage:** remove duplicate error callback ([#1366](https://github.com/aws-amplify/amplify-android/issues/1366))
- **auth:** throw correct auth exception for code mismatch ([#1370](https://github.com/aws-amplify/amplify-android/issues/1370))
- **datastore:** fix subscription timeout period not increasing ([#1376](https://github.com/aws-amplify/amplify-android/issues/1376))
- **datastore:** Add support for SerializedModel to predicate evaluation ([#1375](https://github.com/aws-amplify/amplify-android/issues/1375))
- **datastore:** merge incoming mutation with existing update mutation ([#1379](https://github.com/aws-amplify/amplify-android/issues/1379))
- **datastore:** Merge mutations when create is followed by update ([#1384](https://github.com/aws-amplify/amplify-android/issues/1384))
- **datastore:** explicitly include id field for update mutations, to support types with custom primary keys ([#1385](https://github.com/aws-amplify/amplify-android/issues/1385))
- increase timeout for subscriptions to be established on slow networks ([#1389](https://github.com/aws-amplify/amplify-android/issues/1389))
- **api:** move error handling to multi-auth operation ([#1399](https://github.com/aws-amplify/amplify-android/issues/1399))
- **analytics:** allow user attributes in identifyUser ([#1306](https://github.com/aws-amplify/amplify-android/issues/1306))
- **api:** expose selectionSet in request builder ([#1440](https://github.com/aws-amplify/amplify-android/issues/1440))
- check for canceled call to fix RxJava crash ([#1441](https://github.com/aws-amplify/amplify-android/issues/1441))
- handle null values for predicates ([#1435](https://github.com/aws-amplify/amplify-android/issues/1435))
- **auth:** check for correct exception type when signing out globally ([#1473](https://github.com/aws-amplify/amplify-android/issues/1473))
- **auth:** null-check username when getting current user ([#1490](https://github.com/aws-amplify/amplify-android/issues/1490))
- **datastore:** ensure attaching nested model schema to SerializedModel ([#1495](https://github.com/aws-amplify/amplify-android/issues/1495))
- **datastore:** timeout period not increasing for flutter ([#1505](https://github.com/aws-amplify/amplify-android/issues/1505))
- **datastore:** Ensure not to parse SerializedCustomType if value is null ([#1513](https://github.com/aws-amplify/amplify-android/issues/1513))
- **geo:** Specify jvm target for kotlin package ([#1546](https://github.com/aws-amplify/amplify-android/issues/1546))
- **api:** replace pluralName with listPluralName & syncPluralName ([#1523](https://github.com/aws-amplify/amplify-android/issues/1523))
- **datastore:** Allow different model types with same ID ([#1541](https://github.com/aws-amplify/amplify-android/issues/1541))
- **datastore:** specify model name when querying with Where.id ([#1548](https://github.com/aws-amplify/amplify-android/issues/1548))
- **api:** Add null check before cancelling Call object. ([#1570](https://github.com/aws-amplify/amplify-android/issues/1570))
- **api:** GraphQL operation is now launched from a new thread ([#1562](https://github.com/aws-amplify/amplify-android/issues/1562))
- **aws-api-appsync:** include nested belongsTo object in query selection set ([#1585](https://github.com/aws-amplify/amplify-android/issues/1585))
- **maplibre-adapter:** add content attribution info to map view ([#1591](https://github.com/aws-amplify/amplify-android/issues/1591))
- **datastore:** Alias table names and make column aliases unique in query ([#1603](https://github.com/aws-amplify/amplify-android/issues/1603))
- **aws-datastore:** halt cascading delete if foreign key not found ([#1614](https://github.com/aws-amplify/amplify-android/issues/1614))
- **aws-api-appsync:** update getModelFields for flutter support ([#1611](https://github.com/aws-amplify/amplify-android/issues/1611))
- **api:** create RequestDecorator in interceptor ([#1623](https://github.com/aws-amplify/amplify-android/issues/1623))
- **api:** prevent thread blocking on subx cancellation ([#1482](https://github.com/aws-amplify/amplify-android/issues/1482))
- **datastore:** SerializedModel returns null for non-nullable list field ([#1665](https://github.com/aws-amplify/amplify-android/issues/1665))
- **datastore:** compare datetime values in consistent format when querying ([#1670](https://github.com/aws-amplify/amplify-android/issues/1670))
- **aws-api:** double-encode plus in url path segments ([#1252](https://github.com/aws-amplify/amplify-android/issues/1252))
- Update logic to extract S3 keys from list api response ([#1706](https://github.com/aws-amplify/amplify-android/issues/1706))
- **datastore:** [#1584](https://github.com/aws-amplify/amplify-android/issues/1584) When DateTimeParseException is not available for lower apis ([#1701](https://github.com/aws-amplify/amplify-android/issues/1701))
- start transfer service as foreground for >= API26 ([#1759](https://github.com/aws-amplify/amplify-android/issues/1759))
- **api:** catch all exceptions when making rest request ([#1827](https://github.com/aws-amplify/amplify-android/issues/1827))
- **api:** allow post request with empty body ([#1864](https://github.com/aws-amplify/amplify-android/issues/1864))
- **data:** disable failing test ([#1922](https://github.com/aws-amplify/amplify-android/issues/1922))
- **geo:** use url builder to pass to signer ([#1927](https://github.com/aws-amplify/amplify-android/issues/1927))
- **auth:** fix auth states when invoking signup related APIs ([#1939](https://github.com/aws-amplify/amplify-android/issues/1939))
- **auth:** SignIn call when already signed in should not always call onSuccess ([#1954](https://github.com/aws-amplify/amplify-android/issues/1954))
- **auth:** fix parity issues in API parameters and return types ([#1989](https://github.com/aws-amplify/amplify-android/issues/1989))
- **auth:** fix authsession ([#2008](https://github.com/aws-amplify/amplify-android/issues/2008))
- **auth:** federation updates ([#2041](https://github.com/aws-amplify/amplify-android/issues/2041))
- **predictions:** add JvmStatic annotation ([#2049](https://github.com/aws-amplify/amplify-android/issues/2049))
- **amplify-tools:** use projectDir for project.file relative paths ([#2011](https://github.com/aws-amplify/amplify-android/issues/2011))
- **auth:** Fixed hub initialization trigger event ([#2059](https://github.com/aws-amplify/amplify-android/issues/2059))
- Bugs related to updating state for multi-part upload ([#2072](https://github.com/aws-amplify/amplify-android/issues/2072))
- **auth:** Synchronous Auth API Calls ([#2088](https://github.com/aws-amplify/amplify-android/issues/2088))
- **multiple:** Visibility changes ([#2091](https://github.com/aws-amplify/amplify-android/issues/2091))
- **auth:** Fix State Machine token race condition ([#2100](https://github.com/aws-amplify/amplify-android/issues/2100))
- Errors across storage and analytics category ([#2096](https://github.com/aws-amplify/amplify-android/issues/2096))
- **auth:** Fix authFlowType getter annotation and correct for null safety ([#2109](https://github.com/aws-amplify/amplify-android/issues/2109))
- **auth:** add missing client and analytics metadata ([#2110](https://github.com/aws-amplify/amplify-android/issues/2110))
- callbacks not invoked when attached using getTransfer api ([#2111](https://github.com/aws-amplify/amplify-android/issues/2111))
- **auth:** device metadata migration ([#2114](https://github.com/aws-amplify/amplify-android/issues/2114))
- **core:** remove unused dynamic nav dependency ([#2132](https://github.com/aws-amplify/amplify-android/issues/2132))
- **datastore:** remove typename from ModelMetadata ([#2122](https://github.com/aws-amplify/amplify-android/issues/2122))
- Change order of updating state in local cache ([#2141](https://github.com/aws-amplify/amplify-android/issues/2141))
- fix integration test and added logger to integration test ([#2143](https://github.com/aws-amplify/amplify-android/issues/2143))
- Add missing apis in storage Kotlin & RxJava facade ([#2160](https://github.com/aws-amplify/amplify-android/issues/2160))
- user metadata was persisted empty in the database ([#2165](https://github.com/aws-amplify/amplify-android/issues/2165))
- **geo:** Increase Geo timeout so that it runs successfully on a Pixel 3a XL ([#2177](https://github.com/aws-amplify/amplify-android/issues/2177))
- **Auth:** Fix for when loading credentials the success/error is fired twice ([#2184](https://github.com/aws-amplify/amplify-android/issues/2184))
- **datastore:** Fix lock contention issue when running DataStore.start() from the callback of DataStore.stop() ([#2208](https://github.com/aws-amplify/amplify-android/issues/2208))
- **core:** Remove unused dependencies ([#2207](https://github.com/aws-amplify/amplify-android/issues/2207))
- **geo:** Bump MapLibre SDK to 9.6.0 ([#2254](https://github.com/aws-amplify/amplify-android/issues/2254))
- **analytics:** Remove test dependencies from implementation configuration ([#2253](https://github.com/aws-amplify/amplify-android/issues/2253))
- **auth:** Fix Authorization header for HostedUI fetchToken when appSecret is used ([#2264](https://github.com/aws-amplify/amplify-android/issues/2264))
- **auth:** Moving credential provider to main ([#2273](https://github.com/aws-amplify/amplify-android/issues/2273))
- **auth:** fix npe in initialize fetch auth session ([#2284](https://github.com/aws-amplify/amplify-android/issues/2284))
- **auth:** Fix confirm signin when incorrect MFA code is entered ([#2286](https://github.com/aws-amplify/amplify-android/issues/2286))
- **datastore:** Fix aliasing of column names ([#2312](https://github.com/aws-amplify/amplify-android/issues/2312))

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
- Hub, AmplifyOperation and Event Implementation ([#12](https://github.com/aws-amplify/amplify-android/issues/12))
- Enable remaining class design checkstyles ([#18](https://github.com/aws-amplify/amplify-android/issues/18))
- Enable Java 8 source features ([#20](https://github.com/aws-amplify/amplify-android/issues/20))
- Require Javadocs on all public and protected software entities ([#22](https://github.com/aws-amplify/amplify-android/issues/22))
- Adds AWS S3 storage plugin
- Adds UploadFile operation to AWS S3 Plugin
- Adds support for adding metadata to an upload in AWS S3 Storage Plugin
- Fixes bug with AWS S3 Storage Plugin DownloadFile
- Adds Remove Operation to S3 Storage Plugin
- Refactors Storage Operations to be specific again to handle operations without cancel/resume
- Adds List operation to S3 Storage Plugin
- Remove stale docs tree ([#29](https://github.com/aws-amplify/amplify-android/issues/29))
- Use latest Gradle and Gradle Wrapper ([#30](https://github.com/aws-amplify/amplify-android/issues/30))
- Resolve current checkstyle errors ([#31](https://github.com/aws-amplify/amplify-android/issues/31))
- Miscellaneous code cleanup
- Refactoring of internal Hub plugin implementation details
- CircleCI initial setup ([#27](https://github.com/aws-amplify/amplify-android/issues/27))
- Category/Plugin configuration for core + S3 Storage Plugin configuration ([#35](https://github.com/aws-amplify/amplify-android/issues/35))
- [API] API core framework
- [API] OkHttp-based API plugin supporting GraphQL ([#37](https://github.com/aws-amplify/amplify-android/issues/37))
- [API] Add ApiOperation + refactor Query ([#38](https://github.com/aws-amplify/amplify-android/issues/38))
- Minor fixes
- Fix null casting error in GsonResponseFactory ([#42](https://github.com/aws-amplify/amplify-android/issues/42))
- [API] Add variable support ([#41](https://github.com/aws-amplify/amplify-android/issues/41))
- [API] Support other authorization modes ([#43](https://github.com/aws-amplify/amplify-android/issues/43))
- [Core] Introduce Amplify Operation Request ([#25](https://github.com/aws-amplify/amplify-android/issues/25))
- Cleanup callback, listener, subscriber language ([#48](https://github.com/aws-amplify/amplify-android/issues/48))
- Latest-n-greatest AWS SDKs, versions 2.16.2. ([#49](https://github.com/aws-amplify/amplify-android/issues/49))
- [DataStore] Add Model, ModelSchema and StorageAdapter interface ([#44](https://github.com/aws-amplify/amplify-android/issues/44))
- Use a single version of the AWS SDK across all modules ([#50](https://github.com/aws-amplify/amplify-android/issues/50))
- [DataStore] Add SQLiteHelper and setUp implementation ([#51](https://github.com/aws-amplify/amplify-android/issues/51))
- [DataStore] Observable APIs for DataStore and LocalStorageAdapter ([#52](https://github.com/aws-amplify/amplify-android/issues/52))
- Updates S3 Storage Plugin with proper escape hatch and nonblocking operations
- [DataStore] CreateSqlCommand ([#59](https://github.com/aws-amplify/amplify-android/issues/59))
- [DataStore] Basic plumbing for Sync Engine ([#55](https://github.com/aws-amplify/amplify-android/issues/55))
- Class and member scoping cleanup ([#63](https://github.com/aws-amplify/amplify-android/issues/63))
- [Core] Generic plugin management logic in Amplify facade ([#64](https://github.com/aws-amplify/amplify-android/issues/64))
- [DataStore] Support creation of database indexes through @Index annotation ([#62](https://github.com/aws-amplify/amplify-android/issues/62))
- Don't use returnDefaultValues in testOptions ([#65](https://github.com/aws-amplify/amplify-android/issues/65))
- Add path to store test results ([#67](https://github.com/aws-amplify/amplify-android/issues/67))
- [API] Base code for building GraphQL request dynamically
- [WIP] [StorageAdapter] SetUp and Save operation ([#66](https://github.com/aws-amplify/amplify-android/issues/66))
- Move Model and annotations package into amplify-core
- Fixes getContent method of GraphQLRequest
- Initial mutation doc body
- Add implementation for query ([#69](https://github.com/aws-amplify/amplify-android/issues/69))
- Update API names, codegen for mutation
- Removes unused Enum interface
- Addresses checkStyle issues
- Fixes sync engine test
- Removed wrongly commited amplify config
- Fixes name of exception variable
- Adds DELETE functionality
- [StorageAdapter] Cleanup and fix race condition in setUp ([#73](https://github.com/aws-amplify/amplify-android/issues/73))
- Changes primitive types to Object types
- [API] Support WebSocket-based GraphQL subscriptions ([#72](https://github.com/aws-amplify/amplify-android/issues/72))
- Updates enum to no longer depend on Gson
- Miscellaneous code cleanups ([#75](https://github.com/aws-amplify/amplify-android/issues/75))
- Resolves linter issues and adds back codegen test
- Add SqliteDataType enum ([#76](https://github.com/aws-amplify/amplify-android/issues/76))
- Addresses PR comments
- Increase operation timeout in failing SyncEngine test
- [API] Initial predicate code supporting field equality operation ([#80](https://github.com/aws-amplify/amplify-android/issues/80))
- [DataStore] Add ModelProvider; Add category & plugin infrastructure for DataStore ([#77](https://github.com/aws-amplify/amplify-android/issues/77))
- [DataStore] Added connection and foreign keys ([#81](https://github.com/aws-amplify/amplify-android/issues/81))
- Adds query GET operation generator code ([#82](https://github.com/aws-amplify/amplify-android/issues/82))
- Updates sample model to have identical property names to schema
- [DataStore] Enable communication between StorageEngine and SyncEngine ([#83](https://github.com/aws-amplify/amplify-android/issues/83))
- [DataStore] Fix inconsistencies in tests ([#85](https://github.com/aws-amplify/amplify-android/issues/85))
- [API] Overrides equals and hashcode on Model ([#87](https://github.com/aws-amplify/amplify-android/issues/87))
- [API] Split integration tests that use different models ([#84](https://github.com/aws-amplify/amplify-android/issues/84))
- Adds correct serialization for Date properties ([#89](https://github.com/aws-amplify/amplify-android/issues/89))
- Completes Predicate Structure Code ([#90](https://github.com/aws-amplify/amplify-android/issues/90))
- [Tests] Separate utilities from shared models ([#91](https://github.com/aws-amplify/amplify-android/issues/91))
- [TestUtils] Create a re-usable LatchedResultListener ([#92](https://github.com/aws-amplify/amplify-android/issues/92))
- Codegen for Query LIST type + predicate translation ([#94](https://github.com/aws-amplify/amplify-android/issues/94))
- [DataStore] Add lifecycle management and support for enums ([#86](https://github.com/aws-amplify/amplify-android/issues/86))
- [Core] Wire ModelSchema for equality and collection hashing ([#95](https://github.com/aws-amplify/amplify-android/issues/95))
- Raphk/sqlite bug fix ([#96](https://github.com/aws-amplify/amplify-android/issues/96))
- [Model] Fail defensively on type converstion errors ([#97](https://github.com/aws-amplify/amplify-android/issues/97))
- [DataStore] Publish enqueued DataStore changes via API ([#99](https://github.com/aws-amplify/amplify-android/issues/99))
- Re-implement foreign-key logic ([#98](https://github.com/aws-amplify/amplify-android/issues/98))
- [API] Adds support for GraphQL results with multiple items ([#100](https://github.com/aws-amplify/amplify-android/issues/100))
- [DataStore] Move initialization into configure, and add e2e test ([#101](https://github.com/aws-amplify/amplify-android/issues/101))
- Update analytics interface
- Adds codegen for subscriptions in the non authorized case ([#105](https://github.com/aws-amplify/amplify-android/issues/105))
- Update plugin to match interface
- Adds optional mutation conditions and makes condition optional for query ([#107](https://github.com/aws-amplify/amplify-android/issues/107))
- [Infrastructure] Add gradle-maven publish scripts ([#104](https://github.com/aws-amplify/amplify-android/issues/104))
- Segregate SQLite related metadata from ModelSchema and ModelField ([#103](https://github.com/aws-amplify/amplify-android/issues/103))
- [DataStore] Simplification to Record deserialization ([#109](https://github.com/aws-amplify/amplify-android/issues/109))
- Implement update model flow ([#108](https://github.com/aws-amplify/amplify-android/issues/108))
- [DataStore] Fix model to support multiple (or zero) indexes per schema ([#112](https://github.com/aws-amplify/amplify-android/issues/112))
- Basic belongsTo support without fields param on connection ([#111](https://github.com/aws-amplify/amplify-android/issues/111))
- Support deletion and subscription ([#110](https://github.com/aws-amplify/amplify-android/issues/110))
- [DataStore] Subscribe to API changes via SyncEngine ([#114](https://github.com/aws-amplify/amplify-android/issues/114))
- [DataStore] Increase timeouts in model synchronization tests ([#116](https://github.com/aws-amplify/amplify-android/issues/116))
- [Test] Add Recommended config for Robolectric ([#117](https://github.com/aws-amplify/amplify-android/issues/117))
- Analytics configuration and record event API
- Revert "Analytics configuration and record event API"
- GraphQLRequest generation string case fix ([#119](https://github.com/aws-amplify/amplify-android/issues/119))
- [DataStore] Support target naming foreign key ([#115](https://github.com/aws-amplify/amplify-android/issues/115))
- Review comments
- Fix checkstyle failures
- [DataStore] Implement eager loading foreign key on a query ([#120](https://github.com/aws-amplify/amplify-android/issues/120))
- [API] Further refines GraphQLRequest Generation for Relationships ([#121](https://github.com/aws-amplify/amplify-android/issues/121))
- [DataStore] Support query predicates ([#122](https://github.com/aws-amplify/amplify-android/issues/122))
- [API] Adds tests/fixes for hasOne and many to many relationships ([#124](https://github.com/aws-amplify/amplify-android/issues/124))
- [Testing] Updates test models to latest model codegen
- [API] Update API key used for GraphQLInstrumentationTest ([#128](https://github.com/aws-amplify/amplify-android/issues/128))
- [API] Remove unused import causing checkstyle violation ([#130](https://github.com/aws-amplify/amplify-android/issues/130))
- [Testing] Updates test models to latest codegen
- [API] Removes Unused Subscribe with Predicate ([#131](https://github.com/aws-amplify/amplify-android/issues/131))
- [Core] Add logic to noitfy upon host availability ([#125](https://github.com/aws-amplify/amplify-android/issues/125))
- [Logging] Add a simple logging plugin ([#129](https://github.com/aws-amplify/amplify-android/issues/129))
- Remove AWSMobileClient initialization logic from plugin implementation ([#56](https://github.com/aws-amplify/amplify-android/issues/56))
- Delete unnecessary file from last commit
- [API] Instrumentation Tests can Build and Run ([#134](https://github.com/aws-amplify/amplify-android/issues/134))
- [API] Document the output of the AppSync Request Factory ([#135](https://github.com/aws-amplify/amplify-android/issues/135))
- [API] Use a stable ordering for fields in GraphQL requests ([#136](https://github.com/aws-amplify/amplify-android/issues/136))
- [TestModels] Organize code-generated models into directories ([#133](https://github.com/aws-amplify/amplify-android/issues/133))
- [TestUtils] Simplify the Test Latch APIs ([#137](https://github.com/aws-amplify/amplify-android/issues/137))
- [TestModels] Fix Broken hashCode() on Generated Models ([#139](https://github.com/aws-amplify/amplify-android/issues/139))
- [DataStore] Use prepared statement for query ([#126](https://github.com/aws-amplify/amplify-android/issues/126))
- [API] Allow deserialization of a List of String into an array response ([#138](https://github.com/aws-amplify/amplify-android/issues/138))
- Matched configuration keys ([#141](https://github.com/aws-amplify/amplify-android/issues/141))
- [DataStore] Support persisting and upgrading model version ([#132](https://github.com/aws-amplify/amplify-android/issues/132))
- [Framework] Exception handling overhaul ([#140](https://github.com/aws-amplify/amplify-android/issues/140))
- Initial commit for GET query in API Category
- Addressed PR changes
- Fixed the tests
- Added check style fix
- [DataStore] ModelSync Contract and Data Objects ([#127](https://github.com/aws-amplify/amplify-android/issues/127))
- [DataStore] Update Code Base to Use New Code Gen Models & API ([#142](https://github.com/aws-amplify/amplify-android/issues/142))
- Analytics configuration and record event api ([#118](https://github.com/aws-amplify/amplify-android/issues/118))
- Support for API Key auth in REST ([#146](https://github.com/aws-amplify/amplify-android/issues/146))
- [DataStore] API Interface ([#149](https://github.com/aws-amplify/amplify-android/issues/149))
- Add awsconfiguration json and move amplifyconfiguration json to androidTest ([#150](https://github.com/aws-amplify/amplify-android/issues/150))
- [StorageEngine] Chain upgrade models async calls using flatMap ([#147](https://github.com/aws-amplify/amplify-android/issues/147))
- Update configuration structure reduce auto flush interval for tests ([#151](https://github.com/aws-amplify/amplify-android/issues/151))
- [Hub] Rename plugin to AWSHubPlugin ([#152](https://github.com/aws-amplify/amplify-android/issues/152))
- Added Beta tag to title
- [DataStore] Use Raw Strings Against API Category Behavior ([#154](https://github.com/aws-amplify/amplify-android/issues/154))
- Overload API methods to not require API name parameter ([#148](https://github.com/aws-amplify/amplify-android/issues/148))
- [DataStore] All API Interface Methods tested and working ([#156](https://github.com/aws-amplify/amplify-android/issues/156))
- [DataStore] List deserialization as detail of response deserialization ([#157](https://github.com/aws-amplify/amplify-android/issues/157))
- Update README.md ([#155](https://github.com/aws-amplify/amplify-android/issues/155))
- [DataStore] Fixes linting error ([#158](https://github.com/aws-amplify/amplify-android/issues/158))
- Update broken link on README.md ([#159](https://github.com/aws-amplify/amplify-android/issues/159))
- redact credentials ([#160](https://github.com/aws-amplify/amplify-android/issues/160))
- Update README.md
- Add changelog ([#162](https://github.com/aws-amplify/amplify-android/issues/162))
- [DataStore] Implement delete cascade ([#167](https://github.com/aws-amplify/amplify-android/issues/167))
- [Storage] Fixes config keys ([#168](https://github.com/aws-amplify/amplify-android/issues/168))
- Update links in README ([#161](https://github.com/aws-amplify/amplify-android/issues/161))
- Added headers for rest request ([#165](https://github.com/aws-amplify/amplify-android/issues/165))
- [DataStore] Create RemoteModelState class ([#166](https://github.com/aws-amplify/amplify-android/issues/166))
- [DataStore] Remove API name logic ([#164](https://github.com/aws-amplify/amplify-android/issues/164))
- [DataStore] Removes condition from failing integration test ([#172](https://github.com/aws-amplify/amplify-android/issues/172))
- Update artifact description to indicate preview version ([#173](https://github.com/aws-amplify/amplify-android/issues/173))
- Update changelog ([#174](https://github.com/aws-amplify/amplify-android/issues/174))
- Fix group id typo ([#175](https://github.com/aws-amplify/amplify-android/issues/175))
- Update core artifact id ([#176](https://github.com/aws-amplify/amplify-android/issues/176))
- [Core] Move Immutable helper to utils Java package ([#180](https://github.com/aws-amplify/amplify-android/issues/180))
- Enable integration tests in CircleCI ([#163](https://github.com/aws-amplify/amplify-android/issues/163))
- [Core] Cleanup code quality issues in Amplify facade ([#181](https://github.com/aws-amplify/amplify-android/issues/181))
- Update README.md ([#189](https://github.com/aws-amplify/amplify-android/issues/189))
- Update README.md ([#184](https://github.com/aws-amplify/amplify-android/issues/184))
- [CircleCI] Update scripts for pulling remote configs ([#183](https://github.com/aws-amplify/amplify-android/issues/183))
- Adds field for denoting owner based authorization ([#188](https://github.com/aws-amplify/amplify-android/issues/188))
- Fix test for invalid foreign key ([#190](https://github.com/aws-amplify/amplify-android/issues/190))
- [DataStore] Add Data Hydrator ([#177](https://github.com/aws-amplify/amplify-android/issues/177))
- [DataStore] Fix import of Immutable ([#192](https://github.com/aws-amplify/amplify-android/issues/192))
- [Analytics] Add support for global event properties ([#153](https://github.com/aws-amplify/amplify-android/issues/153))
- [DataStore] Implement conditional save for Storage Engine ([#191](https://github.com/aws-amplify/amplify-android/issues/191))
- [SQLiteStorageAdapter] Minor refactoring ([#194](https://github.com/aws-amplify/amplify-android/issues/194))
- [Core] Lambdas in StreamListener, ResultListener ([#197](https://github.com/aws-amplify/amplify-android/issues/197))
- [TestUtils] Recycle Synchronous Latching Code ([#201](https://github.com/aws-amplify/amplify-android/issues/201))
- [Core] Tighten Type Bounds on Error Consumers ([#200](https://github.com/aws-amplify/amplify-android/issues/200))
- [Storage] Split success and error callbacks ([#205](https://github.com/aws-amplify/amplify-android/issues/205))
- Use Retrolambda for broader consumer compatibility ([#204](https://github.com/aws-amplify/amplify-android/issues/204))
- [API] Accept Lambdas for REST Behaviours ([#206](https://github.com/aws-amplify/amplify-android/issues/206))
- [API] Accept Lambdas in GraphQL Behaviors ([#218](https://github.com/aws-amplify/amplify-android/issues/218))
- [DataStore] Implement in-memory predicate evaluator ([#196](https://github.com/aws-amplify/amplify-android/issues/196))
- [DataStore] Pure Lambda API for DataStore ([#207](https://github.com/aws-amplify/amplify-android/issues/207))
- [Publishing] Consolidate Gradle Hooks for Maven Publishing ([#208](https://github.com/aws-amplify/amplify-android/issues/208))
- Restore build (two separate git commits) ([#222](https://github.com/aws-amplify/amplify-android/issues/222))
- Fully remove ResultListener and StreamListener from code base ([#224](https://github.com/aws-amplify/amplify-android/issues/224))
- [API] IAM support for REST api request ([#178](https://github.com/aws-amplify/amplify-android/issues/178))
- Make direct use of sets (instead of iterators) in storage engine test ([#226](https://github.com/aws-amplify/amplify-android/issues/226))
- [DataStore] Implement conditional delete for storage engine ([#198](https://github.com/aws-amplify/amplify-android/issues/198))
- [DataStore] Refactor in-memory storage adapter ([#227](https://github.com/aws-amplify/amplify-android/issues/227))
- [DataStore] Always close SQLiteCursor in instrumentation test ([#228](https://github.com/aws-amplify/amplify-android/issues/228))
- [API] Fixes [#187](https://github.com/aws-amplify/amplify-android/issues/187): Subscription blocking + web socket connection error not correctly reported ([#217](https://github.com/aws-amplify/amplify-android/issues/217))
- Update checkstyle rules to accept variable copyright year ([#231](https://github.com/aws-amplify/amplify-android/issues/231))
- Overhaul module build scripts ([#235](https://github.com/aws-amplify/amplify-android/issues/235))
- Use xlarge resource class for CircleCI build
- [CircleCI] Add a warning in case AWS credentials not available
- OkHttp 4.3.0 -> 4.3.1
- Add User-Agent header to all outbound network requests
- Use latest Checkstyle version 8.28.
- Bubble up consumed failures in synchronous test utils
- Generate AmplifyConfiguration from a factory method ([#239](https://github.com/aws-amplify/amplify-android/issues/239))
- [core] Add an initialize method to the Category and Plugin ([#246](https://github.com/aws-amplify/amplify-android/issues/246))
- Use latest AWS SDK release 2.16.7 ([#255](https://github.com/aws-amplify/amplify-android/issues/255))
- [DataStore] Use Hub notifications to communicate Sync Engine events ([#251](https://github.com/aws-amplify/amplify-android/issues/251))
- [Storage] Refactor and component test ([#254](https://github.com/aws-amplify/amplify-android/issues/254))
- Fix the error that caused configuration to fail unless mobile client was initialized ([#257](https://github.com/aws-amplify/amplify-android/issues/257))
- Make builder methods public accessible ([#258](https://github.com/aws-amplify/amplify-android/issues/258))
- Update copy-configs ([#260](https://github.com/aws-amplify/amplify-android/issues/260))
- Patch flaky tests for CI/CD ([#263](https://github.com/aws-amplify/amplify-android/issues/263))
- Update README.md ([#261](https://github.com/aws-amplify/amplify-android/issues/261))
- Resolves some issues found by LGTM.com. ([#267](https://github.com/aws-amplify/amplify-android/issues/267))
- [aws-storage-s3] Suppress autovalue warning from Robolectric ([#266](https://github.com/aws-amplify/amplify-android/issues/266))
- [core] unit tests and tweaks for Category ([#250](https://github.com/aws-amplify/amplify-android/issues/250))
- [aws-datastore] Synchronous adapter utility for instrumentation tests ([#265](https://github.com/aws-amplify/amplify-android/issues/265))
- [core] Move NoOpCancelable into core module ([#269](https://github.com/aws-amplify/amplify-android/issues/269))
- [aws-datastore] Align names of SyncEngine components to iOS, JavaScript ([#272](https://github.com/aws-amplify/amplify-android/issues/272))
- [aws-datastore] Separate Sync Engine from App Sync code ([#271](https://github.com/aws-amplify/amplify-android/issues/271))
- [rxbindings] Add an RxJava2 facade ([#268](https://github.com/aws-amplify/amplify-android/issues/268))
- Use latest dependency versions ([#275](https://github.com/aws-amplify/amplify-android/issues/275))
- Fixes to accomodate consuming Amplify from an app ([#283](https://github.com/aws-amplify/amplify-android/issues/283))
- [aws-datastore] CRUD type mapping is backwards ([#284](https://github.com/aws-amplify/amplify-android/issues/284))
- Use try-with-resources statement to assure that cursor is closed ([#282](https://github.com/aws-amplify/amplify-android/issues/282))
- Refactor Storage operations and service ([#286](https://github.com/aws-amplify/amplify-android/issues/286))
- Refactor storage options to abstract out common properties ([#285](https://github.com/aws-amplify/amplify-android/issues/285))
- [Storage] Support getUrl() ([#248](https://github.com/aws-amplify/amplify-android/issues/248))
- [testutils] Add synchronous storage and mobile client to testutils ([#281](https://github.com/aws-amplify/amplify-android/issues/281))
- Use "master" as the default version name ([#288](https://github.com/aws-amplify/amplify-android/issues/288))
- Add storage instrumentation tests ([#290](https://github.com/aws-amplify/amplify-android/issues/290))
- [aws-datastore] Wait for initialization before attempting operations ([#287](https://github.com/aws-amplify/amplify-android/issues/287))
- [aws-datastore] Surface more detail in exception when sync engine fails to publish
- [aws-datastore] Clarify nullability of fields in ModelSchema, SQLiteTable
- [aws-datastore] Cleanup error handling in SQLiteStorageAdapter
- [aws-datastore] System models now provided external to storage adapter
- Update contributors guide ([#299](https://github.com/aws-amplify/amplify-android/issues/299))
- [Storage] Make list return Amplify, not service, key ([#291](https://github.com/aws-amplify/amplify-android/issues/291))
- Don't rename source file attribute in ProGuard ([#305](https://github.com/aws-amplify/amplify-android/issues/305))
- Android Gradle Plugin to 3.6.1. ([#306](https://github.com/aws-amplify/amplify-android/issues/306))
- [Analytics] Support Auto session tracking initial commit ([#233](https://github.com/aws-amplify/amplify-android/issues/233))
- [aws-api] @Ignore subscribeFailsWithoutProperAuth ([#310](https://github.com/aws-amplify/amplify-android/issues/310))
- Use API 29 emulator for CircleCI runs
- Revert "Use API 29 emulator for CircleCI runs"
- Retain insertion order of categories in Amplify facade ([#307](https://github.com/aws-amplify/amplify-android/issues/307))
- Fix path to copy-configs script ([#317](https://github.com/aws-amplify/amplify-android/issues/317))
- [aws-storage-s3] Temporarily ignore upload and download tests ([#318](https://github.com/aws-amplify/amplify-android/issues/318))
- [aws-datastore] Perform base sync at startup and save model versions
- [aws-datastore] Separate create and update types for storage adapter
- [aws-datastore] Don't ignore update and delete outbox tests
- [aws-datastore] Handle create update and delete in MutationProcessor
- [aws-datastore] All network data passed through merger
- Add Android device details to User-Agent
- [Analytics] Add api to attach user information to the endpoint ([#312](https://github.com/aws-amplify/amplify-android/issues/312))
- [aws-datastore] Re-enable test for cloud to local synchronization
- [aws-api] Use wants to provide multiple path segments
- Remove Retrolambda
- Use the latest Gradle release, 6.2.2.
- [aws-api] Test two subscriptions for the same thing have unqiue ACKs
- Raise minSdkVersion to 16 ([#327](https://github.com/aws-amplify/amplify-android/issues/327))
- Update the plugin key for analytics to match one generated by the cli and update configuration method ([#328](https://github.com/aws-amplify/amplify-android/issues/328))
- Temporarily ignore broken test ([#332](https://github.com/aws-amplify/amplify-android/issues/332))
- Plugin configuration requirement removed ([#333](https://github.com/aws-amplify/amplify-android/issues/333))
- Advertise version 0.10.0 in the README.md.
- Fixes Date fields being converted to current date ([#335](https://github.com/aws-amplify/amplify-android/issues/335))
- [Predictions] core models for interpret ([#331](https://github.com/aws-amplify/amplify-android/issues/331))
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
- Minimize accessibility whereever possible ([#329](https://github.com/aws-amplify/amplify-android/issues/329))
- [core] Miscellaneous lint cleanups and spelling fixes
- [aws-datastore] Single responsibility for SynchronousStorageAdapter
- Implement offline interpret ([#336](https://github.com/aws-amplify/amplify-android/issues/336))
- [aws-api] Run tests against ApiCategory, not Amplify ([#354](https://github.com/aws-amplify/amplify-android/issues/354))
- [aws-storage-s3] Test against StorageCategory not Amplify ([#355](https://github.com/aws-amplify/amplify-android/issues/355))
- [aws-datastore] Test against DataStoreCategory not Amplify ([#362](https://github.com/aws-amplify/amplify-android/issues/362))
- Transitive dependencies already included for AWS Mobile Client ([#359](https://github.com/aws-amplify/amplify-android/issues/359))
- Use Latest SDK, 2.16.11. ([#361](https://github.com/aws-amplify/amplify-android/issues/361))
- Implement interpret with AWS Comprehend ([#346](https://github.com/aws-amplify/amplify-android/issues/346))
- Run tests against PredictionsCategory, not Amplify ([#364](https://github.com/aws-amplify/amplify-android/issues/364))
- Add unit tests for offline predictions ([#363](https://github.com/aws-amplify/amplify-android/issues/363))
- Improve feature comparison in tests ([#365](https://github.com/aws-amplify/amplify-android/issues/365))
- Re-enable ignored storage test ([#367](https://github.com/aws-amplify/amplify-android/issues/367))
- [aws-analytics-pinpoint] Introduce builder for AnalyticsEvent ([#353](https://github.com/aws-amplify/amplify-android/issues/353))
- Implement online translate using AWS Translate ([#369](https://github.com/aws-amplify/amplify-android/issues/369))
- [core] Handle missing configuration file ([#376](https://github.com/aws-amplify/amplify-android/issues/376))
- [testutils] Prevent deadlocks in Latch and Await ([#375](https://github.com/aws-amplify/amplify-android/issues/375))
- Enforce a consistent annotation specification style ([#372](https://github.com/aws-amplify/amplify-android/issues/372))
- Basic cleanups in the README.md ([#381](https://github.com/aws-amplify/amplify-android/issues/381))
- [aws-datastore] Move AppSync type enumerations out of core ([#385](https://github.com/aws-amplify/amplify-android/issues/385))
- [aws-datastore] Maintain insertion order in ModelProviders ([#389](https://github.com/aws-amplify/amplify-android/issues/389))
- [aws-datastore] Log inbound merge events as information ([#390](https://github.com/aws-amplify/amplify-android/issues/390))
- Use File instead of String input parameters ([#391](https://github.com/aws-amplify/amplify-android/issues/391))
- [aws-api] Use latest OkHttp3, 4.5.0. ([#387](https://github.com/aws-amplify/amplify-android/issues/387))
- Android Gradle Plugin to 3.6.3 ([#388](https://github.com/aws-amplify/amplify-android/issues/388))
- [aws-datastore] AWSDataStorePlugin via Constructor ([#397](https://github.com/aws-amplify/amplify-android/issues/397))
- Expose all GraphQL error properties ([#392](https://github.com/aws-amplify/amplify-android/issues/392))
- Implement online celebrity + label detection ([#394](https://github.com/aws-amplify/amplify-android/issues/394))
- Expose all GraphQL error properties (part 2) ([#400](https://github.com/aws-amplify/amplify-android/issues/400))
- Add MissingJavadocType to checkstyle-rules ([#403](https://github.com/aws-amplify/amplify-android/issues/403))
- Add Javadoc to resolve checkstyle error ([#404](https://github.com/aws-amplify/amplify-android/issues/404))
- Temporarily disable failing REST API test ([#411](https://github.com/aws-amplify/amplify-android/issues/411))
- Cleanups to README, CHANGELOG, CONTRIBUTING ([#413](https://github.com/aws-amplify/amplify-android/issues/413))
- Implement online entity detection ([#399](https://github.com/aws-amplify/amplify-android/issues/399))
- A few small doc tweaks to README.md, CONTRIBUTING.md. ([#415](https://github.com/aws-amplify/amplify-android/issues/415))
- [aws-api] Basic GraphQL components tests for AWSApiPlugin ([#414](https://github.com/aws-amplify/amplify-android/issues/414))
- [aws-datstore] Implementation of user-provided DataStore configs ([#401](https://github.com/aws-amplify/amplify-android/issues/401))
- fix datastore javadoc checkstyle ([#418](https://github.com/aws-amplify/amplify-android/issues/418))
- add support for non-model types in DataStore ([#398](https://github.com/aws-amplify/amplify-android/issues/398))
- Fix JsonParseException when parsing null path in Error object ([#412](https://github.com/aws-amplify/amplify-android/issues/412))
- Flip constant equality checks ([#420](https://github.com/aws-amplify/amplify-android/issues/420))
- [aws-datastore] Rename Mutation as SubscriptionEvent ([#419](https://github.com/aws-amplify/amplify-android/issues/419))
- Miscellaneous changes for Predictions ([#421](https://github.com/aws-amplify/amplify-android/issues/421))
- [aws-api] Handle AppSync date time scalar types ([#407](https://github.com/aws-amplify/amplify-android/issues/407))
- Add unit test for result transfomer utilities ([#422](https://github.com/aws-amplify/amplify-android/issues/422))
- Format JSON so it is more readable ([#423](https://github.com/aws-amplify/amplify-android/issues/423))
- Implement online text detection  ([#409](https://github.com/aws-amplify/amplify-android/issues/409))
- [aws-api] Reinstate List, Separate Date and AWSTimestamp
- [aws-datastore] Multidex support for API levels 16-21
- [aws-storage-s3] Ignore flaky resume test
- [aws-datastore] Ignore MutationProcessorTest#canDrainMutationBox ([#429](https://github.com/aws-amplify/amplify-android/issues/429))
- [core] Ignore HubInstrumentedTest#multiplePublications ([#430](https://github.com/aws-amplify/amplify-android/issues/430))
- Do not require a particular profile to run copy-configs ([#428](https://github.com/aws-amplify/amplify-android/issues/428))
- [aws-datastore] Only start Orchestrator if API configured ([#425](https://github.com/aws-amplify/amplify-android/issues/425))
- [aws-datastore] Decompose StorageItemChange into smaller pieces
- [aws-datastore] Emit StorageItemChange from LocalStorageAdapter
- [aws-datastore] Spilt StorageItemChange and PendingMutation
- Add unit tests for predictions result transformers ([#426](https://github.com/aws-amplify/amplify-android/issues/426))
- [aws-datastore] Move model concerns into model subpackage ([#435](https://github.com/aws-amplify/amplify-android/issues/435))
- [core] Remove a superfluous utility class ([#436](https://github.com/aws-amplify/amplify-android/issues/436))
- Update CONTRIBUTING.md
- [core] Remove some sparesely or unused constructs ([#440](https://github.com/aws-amplify/amplify-android/issues/440))
- [aws-datastore] PendingMutations are now Comparable according to their creation times ([#439](https://github.com/aws-amplify/amplify-android/issues/439))
- [DataStore] query API changes with pagination support ([#438](https://github.com/aws-amplify/amplify-android/issues/438))
- Adds some miscellaneous, missing method documentation. ([#437](https://github.com/aws-amplify/amplify-android/issues/437))
- [core] Break apart utils classes ([#442](https://github.com/aws-amplify/amplify-android/issues/442))
- [aws-datastore] Test reliability in TimeBasedUuidTest ([#446](https://github.com/aws-amplify/amplify-android/issues/446))
- [aws-datastore] Logging cleanups ([#449](https://github.com/aws-amplify/amplify-android/issues/449))
- [aws-datastore] Clarify AWSDataStorePlugin dependency on API Category ([#450](https://github.com/aws-amplify/amplify-android/issues/450))
- Simplify Hub and HubAccumulator ([#448](https://github.com/aws-amplify/amplify-android/issues/448))
- Implement online text to speech conversion ([#432](https://github.com/aws-amplify/amplify-android/issues/432))
- [DataStore] fix type conversion for enums and dates ([#447](https://github.com/aws-amplify/amplify-android/issues/447))
- Auth category ([#445](https://github.com/aws-amplify/amplify-android/issues/445))
- [aws-datastore] thread safe rx publish subjects ([#454](https://github.com/aws-amplify/amplify-android/issues/454))
- Add Discord badge ([#456](https://github.com/aws-amplify/amplify-android/issues/456))
- Update Auth Session to Final Design ([#457](https://github.com/aws-amplify/amplify-android/issues/457))
- [aws-datastore] Refactor SubscriptionProcessor ([#467](https://github.com/aws-amplify/amplify-android/issues/467))
- [aws-datastore] Fix unhandled exceptions when no API is available ([#459](https://github.com/aws-amplify/amplify-android/issues/459))
- Adds Hub events for Auth ([#462](https://github.com/aws-amplify/amplify-android/issues/462))
- Adds sign out to Auth ([#463](https://github.com/aws-amplify/amplify-android/issues/463))
- [aws-auth-cognito] Fix checkstyle issue ([#471](https://github.com/aws-amplify/amplify-android/issues/471))
- [aws-datastore] Include mutation details in publication exception ([#473](https://github.com/aws-amplify/amplify-android/issues/473))
- [aws-datastore] Remove an unused constant ([#474](https://github.com/aws-amplify/amplify-android/issues/474))
- [aws-datastore] Additional logging ([#476](https://github.com/aws-amplify/amplify-android/issues/476))
- Allow a global log level in the AndroidLoggingPlugin ([#475](https://github.com/aws-amplify/amplify-android/issues/475))
- [aws-analytics-pinpoint] Minor formatting and style tweaks ([#477](https://github.com/aws-amplify/amplify-android/issues/477))
- [aws-datastore] Further refinements to log levels ([#478](https://github.com/aws-amplify/amplify-android/issues/478))
- [aws-datastore] Minor code style rweaks in SQLiteStorageAdapter
- [aws-datastore] three-way merge logic for unconditional mutations ([#460](https://github.com/aws-amplify/amplify-android/issues/460))
- [aws-datastore] Optimize record lookup ([#480](https://github.com/aws-amplify/amplify-android/issues/480))
- Use latest dependency versions ([#482](https://github.com/aws-amplify/amplify-android/issues/482))
- Add text and label configuration ([#481](https://github.com/aws-amplify/amplify-android/issues/481))
- [core] Repackage REST and GraphQL Behaviors interfaces ([#485](https://github.com/aws-amplify/amplify-android/issues/485))
- Adds signInWithWebUI and signInWithSocialWebUI ([#486](https://github.com/aws-amplify/amplify-android/issues/486))
- [aws-datastore] Permit multiple mutations per model ID ([#491](https://github.com/aws-amplify/amplify-android/issues/491))
- [aws-datastore] Remove mutation before merge in MutationProcessor ([#492](https://github.com/aws-amplify/amplify-android/issues/492))
- [aws-datastore] Mark mutations as in-flight while processing ([#495](https://github.com/aws-amplify/amplify-android/issues/495))
- [aws-datastore] Serialize observation of storage ([#493](https://github.com/aws-amplify/amplify-android/issues/493))
- [aws-datastore] Consider the model version before merging ([#494](https://github.com/aws-amplify/amplify-android/issues/494))
- [testutils] Remove logically incorrect HubAccumulator test ([#500](https://github.com/aws-amplify/amplify-android/issues/500))
- Auth Integration for Storage ([#497](https://github.com/aws-amplify/amplify-android/issues/497))
- Auth Integration for API ([#501](https://github.com/aws-amplify/amplify-android/issues/501))
- Auth integration with Analytics ([#503](https://github.com/aws-amplify/amplify-android/issues/503))
- Integrates Auth with Predictions, Updates Sign Out, Disables AWS Logs ([#506](https://github.com/aws-amplify/amplify-android/issues/506))
- [aws-datastore] Improve list-scattering routines in test coe ([#507](https://github.com/aws-amplify/amplify-android/issues/507))
- [aws-api] add pagination ([#488](https://github.com/aws-amplify/amplify-android/issues/488))
- [aws-datastore] Clarify field naming in PersistentRecord ([#508](https://github.com/aws-amplify/amplify-android/issues/508))
- (feat) amplify-tools as a separate project ([#505](https://github.com/aws-amplify/amplify-android/issues/505))
- [core] Add equals hashCode toString for API data classes ([#511](https://github.com/aws-amplify/amplify-android/issues/511))
- [core] Add equals hashCode toString for Analytics data types ([#510](https://github.com/aws-amplify/amplify-android/issues/510))
- Fixes global signout exception handling ([#512](https://github.com/aws-amplify/amplify-android/issues/512))
- Add AmplifyDisposables utility to each modules using Rx ([#509](https://github.com/aws-amplify/amplify-android/issues/509))
- [aws-datastore] Implement DataStore clear feature ([#416](https://github.com/aws-amplify/amplify-android/issues/416))
- Subscription authorizer for API ([#502](https://github.com/aws-amplify/amplify-android/issues/502))
- [aws-datastore] adding predicate parameter to the necessary APIs ([#496](https://github.com/aws-amplify/amplify-android/issues/496))
- [aws-datastore] Handle list responses without any items ([#515](https://github.com/aws-amplify/amplify-android/issues/515))
- Return empty array instead of null for empty items in response ([#516](https://github.com/aws-amplify/amplify-android/issues/516))
- Add equals, hashcode to VariablesSerializers ([#517](https://github.com/aws-amplify/amplify-android/issues/517))
- Rename AWS* to Temporal* in the TemporalDeserializers ([#518](https://github.com/aws-amplify/amplify-android/issues/518))
- [aws-datastore] equals, toString, hashCode for config and conflict handler ([#519](https://github.com/aws-amplify/amplify-android/issues/519))
- Update OkHttp to latest, 4.7.2. ([#520](https://github.com/aws-amplify/amplify-android/issues/520))
- [aws-api] Move mockwebserver to the test dependencies section ([#521](https://github.com/aws-amplify/amplify-android/issues/521))
- Android Studio Reformat (⌥⌘-L)
- Fix IDE integrations for Windows
- Fix modelgen
- Fix amplifyPush
- Set VERSION_NAME to master
- [aws-api] Fix POST requests with non-empty body ([#525](https://github.com/aws-amplify/amplify-android/issues/525))
- Remove AWSCredentialsProvider behavior ([#528](https://github.com/aws-amplify/amplify-android/issues/528))
- Removes allowAccessModification from ProGuard ([#529](https://github.com/aws-amplify/amplify-android/issues/529))
- README Updates for 1.0.0 ([#523](https://github.com/aws-amplify/amplify-android/issues/523))
- Show build outputs and unit test results in CircleCI Web UI ([#534](https://github.com/aws-amplify/amplify-android/issues/534))
- Android Gradle Plugin 4.0.0, Gradle 6.4.1, AndroidX Core 1.3.0.
- Add time constraints on unbounded blocking calls ([#535](https://github.com/aws-amplify/amplify-android/issues/535))
- Fix Getting Started link ([#569](https://github.com/aws-amplify/amplify-android/issues/569))
- ci: Try build and integrationtest 3 times before failing ([#573](https://github.com/aws-amplify/amplify-android/issues/573))
- ci: improve integration test retry logic ([#608](https://github.com/aws-amplify/amplify-android/issues/608))
- vend codecov.io bash script ourselves instead of downloading ([#609](https://github.com/aws-amplify/amplify-android/issues/609))
- [aws-datastore] fix model name plural form creation in AppSyncRequestFactory ([#628](https://github.com/aws-amplify/amplify-android/issues/628))
- GraphQLRequestFactory refactor & add owner based auth support to API category ([#596](https://github.com/aws-amplify/amplify-android/issues/596))
- feat(datastore) Add support for owner based auth ([#635](https://github.com/aws-amplify/amplify-android/issues/635))
- fix(datastore) add deserializers for dates ([#642](https://github.com/aws-amplify/amplify-android/issues/642))
- fix(datastore) Race condition fix and other stability-related fixes ([#599](https://github.com/aws-amplify/amplify-android/issues/599))
- fix(datastore) Missed a function name change during merge ([#643](https://github.com/aws-amplify/amplify-android/issues/643))
- fix(api) Fix tests broken by PR [#599](https://github.com/aws-amplify/amplify-android/issues/599) ([#647](https://github.com/aws-amplify/amplify-android/issues/647))
- fix(datastore) Set isOrchestratorReady flag to true  when no API is configured ([#653](https://github.com/aws-amplify/amplify-android/issues/653))
- chore(datastore) Remove all API deserialization logic in DataStore, and rely on API instead ([#665](https://github.com/aws-amplify/amplify-android/issues/665))
- fix(amplify-tools) added a null check for RunManagerNode by parsing of workspace.xml ([#673](https://github.com/aws-amplify/amplify-android/issues/673))
- Revert "chore: enable coverage reports ([#592](https://github.com/aws-amplify/amplify-android/issues/592))" ([#677](https://github.com/aws-amplify/amplify-android/issues/677))
- Sends proper user agent for Auth ([#661](https://github.com/aws-amplify/amplify-android/issues/661))
- Fixes unit test for new Auth User Agent ([#694](https://github.com/aws-amplify/amplify-android/issues/694))
- Fix broken assert statement ([#705](https://github.com/aws-amplify/amplify-android/issues/705))
- Updates to latest SDK version ([#708](https://github.com/aws-amplify/amplify-android/issues/708))
- feat(datastore) Adding SyncType to LastSyncMetadata ([#713](https://github.com/aws-amplify/amplify-android/issues/713))
- feature(core): allow additional platforms in amplify config ([#703](https://github.com/aws-amplify/amplify-android/issues/703))
- increase jvm max heap size ([#717](https://github.com/aws-amplify/amplify-android/issues/717))
- Fix (datastore) quote sql commands ([#712](https://github.com/aws-amplify/amplify-android/issues/712))
- feature: adds progress callbacks to storage ([#680](https://github.com/aws-amplify/amplify-android/issues/680))
- feat(api) Implement Comparable for Temporal types so they can be used in predicates ([#721](https://github.com/aws-amplify/amplify-android/issues/721))
- feature(datastore) Trigger network-related hub events ([#716](https://github.com/aws-amplify/amplify-android/issues/716))
- Trying to pin the image version ([#733](https://github.com/aws-amplify/amplify-android/issues/733))
- Pull version name from BuildConfig ([#735](https://github.com/aws-amplify/amplify-android/issues/735))
- Revert [#734](https://github.com/aws-amplify/amplify-android/issues/734) ([#736](https://github.com/aws-amplify/amplify-android/issues/736))
- fix(datastore) quote drop table command ([#728](https://github.com/aws-amplify/amplify-android/issues/728))
- feat(api) PaginatedResult now implements Iterable ([#750](https://github.com/aws-amplify/amplify-android/issues/750))
- chore(ci) ignore tests failing due to resources that no longer exist ([#754](https://github.com/aws-amplify/amplify-android/issues/754))
- Resolve a typo in #Contributing via Pull Requests ([#756](https://github.com/aws-amplify/amplify-android/issues/756))
- feat(datastore) add pagination, respecting syncPageSize and syncMaxRecords ([#737](https://github.com/aws-amplify/amplify-android/issues/737))
- feat(DataStore) add sorting ([#633](https://github.com/aws-amplify/amplify-android/issues/633))
- chore(spelling) Duplicate the ([#761](https://github.com/aws-amplify/amplify-android/issues/761))
- chore(core) Enable logging during unit tests ([#744](https://github.com/aws-amplify/amplify-android/issues/744))
- feat(rxbindings) RxBindings improvements. ([#771](https://github.com/aws-amplify/amplify-android/issues/771))
- feat(datastore) Trigger hub events during sync ([#710](https://github.com/aws-amplify/amplify-android/issues/710))
- fix(rxbindings) Make ConnectionStateEvent public ([#774](https://github.com/aws-amplify/amplify-android/issues/774))
- show toast when issue body is copied ([#775](https://github.com/aws-amplify/amplify-android/issues/775))
- Revert "fix(aws-datastore): selection set for nested custom types ([#730](https://github.com/aws-amplify/amplify-android/issues/730))" ([#777](https://github.com/aws-amplify/amplify-android/issues/777))
- Categorize auth errors ([#770](https://github.com/aws-amplify/amplify-android/issues/770))
- ci: improve integration test retry logic
- Increase timeout for AppSyncClientInstrumentationTest
- Amplify release version 1.3.1 ([#797](https://github.com/aws-amplify/amplify-android/issues/797))
- fix(datastore) Add missing converters for AWSTimestamp used by SQLiteStorageAdapter ([#783](https://github.com/aws-amplify/amplify-android/issues/783))
- Revert "Revert "fix(aws-datastore): selection set for nested custom types ([#730](https://github.com/aws-amplify/amplify-android/issues/730))" ([#777](https://github.com/aws-amplify/amplify-android/issues/777))" ([#801](https://github.com/aws-amplify/amplify-android/issues/801))
- fix(api) owner with readonly access should be able to sub to on_delete, on_create, on_update ([#807](https://github.com/aws-amplify/amplify-android/issues/807))
- feature: user can provide a datastore config ([#810](https://github.com/aws-amplify/amplify-android/issues/810))
- Fix configuration of mavenLocal in 'Consuming Development Versions' ([#819](https://github.com/aws-amplify/amplify-android/issues/819))
- Fix a typo ([#820](https://github.com/aws-amplify/amplify-android/issues/820))
- chore(build) Update awsSdkVersion dependency to 2.19.0 ([#828](https://github.com/aws-amplify/amplify-android/issues/828))
- fix(api) Fix ClassCastException when building selection set for custom type ([#829](https://github.com/aws-amplify/amplify-android/issues/829))
- fix(api) Throw exception when constructing Date from invalid input String ([#825](https://github.com/aws-amplify/amplify-android/issues/825))
- chore(datastore) Represent lastChangedAt as Temporal.Timestamp instead of long ([#670](https://github.com/aws-amplify/amplify-android/issues/670))
- chore(aws-api-appsync) Use class as value instead of simpleName for JavaFieldType in case of duplicated Class simple name. ([#839](https://github.com/aws-amplify/amplify-android/issues/839))
- Adds support for mixed owner and group based auth rules ([#860](https://github.com/aws-amplify/amplify-android/issues/860))
- chore(datastore) Remove unused AWS_GRAPH_QL_TO_JAVA map ([#866](https://github.com/aws-amplify/amplify-android/issues/866))
- chore(devmenu) ignore failing instrumentation tests ([#865](https://github.com/aws-amplify/amplify-android/issues/865))
- chore(api) remove JAVA_DATE since codegen does not generate java.lang.Date ([#867](https://github.com/aws-amplify/amplify-android/issues/867))
- Add stale configuration ([#878](https://github.com/aws-amplify/amplify-android/issues/878))
- use the latest release in README ([#882](https://github.com/aws-amplify/amplify-android/issues/882))
- (fix) Fail early and throw useful exception if customer forgets to call Amplify.configure ([#888](https://github.com/aws-amplify/amplify-android/issues/888))
- feature(aws-datastore): handle mutation conflicts ([#883](https://github.com/aws-amplify/amplify-android/issues/883))
- fix(datastore):Prevent concurrent start/stop on orchestrator ([#876](https://github.com/aws-amplify/amplify-android/issues/876))
- Enable setting of the server side encryption algorithm in StorageUploadFileOptions ([#886](https://github.com/aws-amplify/amplify-android/issues/886))
- feature: resolve conflicts according to strategies ([#904](https://github.com/aws-amplify/amplify-android/issues/904))
- Update README to reference latest release 1.4.2 ([#915](https://github.com/aws-amplify/amplify-android/issues/915))
- feature(aws-api): support custom group claim ([#930](https://github.com/aws-amplify/amplify-android/issues/930))
- feat(datastore) Add start and stop, and stop starting on configure and clear ([#909](https://github.com/aws-amplify/amplify-android/issues/909))
- checkstyle fixup ([#941](https://github.com/aws-amplify/amplify-android/issues/941))
- feat(datastore) swallow unauthorized errors for subscriptions ([#942](https://github.com/aws-amplify/amplify-android/issues/942))
- feature(build): Trigger unit and integration tests in CodeBuild ([#927](https://github.com/aws-amplify/amplify-android/issues/927))
- fix(datastore) only fire DataStore Hub READY event if not already started ([#952](https://github.com/aws-amplify/amplify-android/issues/952))
- Add Builder for AuthRule ([#938](https://github.com/aws-amplify/amplify-android/issues/938))
- recursively build joins for multilevel nested models ([#892](https://github.com/aws-amplify/amplify-android/issues/892))
- feature(aws-datastore): support for hybrid platforms ([#954](https://github.com/aws-amplify/amplify-android/issues/954))
- Add upload InputStream API ([#955](https://github.com/aws-amplify/amplify-android/issues/955))
- feat(datastore) selective sync ([#959](https://github.com/aws-amplify/amplify-android/issues/959))
- chore(release) 1.6.2 ([#974](https://github.com/aws-amplify/amplify-android/issues/974))
- chore(rxbindings) second attempt to fix transient test failures ([#990](https://github.com/aws-amplify/amplify-android/issues/990))
- chore(predictions) fix integ tests ([#992](https://github.com/aws-amplify/amplify-android/issues/992))
- fix(datastore) release startStopSemaphore when start returns, not when API sync completes ([#1027](https://github.com/aws-amplify/amplify-android/issues/1027))
- [aws-api] Fix DELETE calls not working with v4 signer ([#1037](https://github.com/aws-amplify/amplify-android/issues/1037))
- feature: parallelize integ test config downloads ([#1042](https://github.com/aws-amplify/amplify-android/issues/1042))
- fix(datastore) query results should be sorted when sort order provided ([#1049](https://github.com/aws-amplify/amplify-android/issues/1049))
- Send unix epoch in OutboxMutationEvent instead of Temporal.Timestamp. ([#1052](https://github.com/aws-amplify/amplify-android/issues/1052))
- MutationProcessor - Fix missing schema on SerializedModel mutations. ([#1051](https://github.com/aws-amplify/amplify-android/issues/1051))
- Update README.md ([#1044](https://github.com/aws-amplify/amplify-android/issues/1044))
- feat(api) response deserialization should only skip top level for specific response types ([#1062](https://github.com/aws-amplify/amplify-android/issues/1062))
- chore(api) remove responseType from GraphQLOperation, since we already know it from the GraphQLRequest ([#1063](https://github.com/aws-amplify/amplify-android/issues/1063))
- Release 1.6.8 ([#1065](https://github.com/aws-amplify/amplify-android/issues/1065))
- fix(datastore) publish networkStatus event at correct times ([#1067](https://github.com/aws-amplify/amplify-android/issues/1067))
- chore(datastore) missing column name should be a verbose log since it is expected for relationship fields ([#1068](https://github.com/aws-amplify/amplify-android/issues/1068))
- chore(datastore) verbose log instead of warn when deleting a non existent item ([#1081](https://github.com/aws-amplify/amplify-android/issues/1081))
- fix(datastore) Defer merger.merge to avoid failure if outbox has mutation ([#1082](https://github.com/aws-amplify/amplify-android/issues/1082))
- fix(datastore) Make PersistentMutationOutbox operations synchronized ([#1085](https://github.com/aws-amplify/amplify-android/issues/1085))
- fix(api) serialize nulls on requests to support setting fields to null ([#1091](https://github.com/aws-amplify/amplify-android/issues/1091))
- chore(datastore) remove overloaded query method in favor of just one ([#1092](https://github.com/aws-amplify/amplify-android/issues/1092))
- chore(release) 1.6.9 ([#1097](https://github.com/aws-amplify/amplify-android/issues/1097))
- refactor sqlite storage adapter ([#1093](https://github.com/aws-amplify/amplify-android/issues/1093))
- Throw AlreadyConfiguredException on reconfiguration attempt ([#1109](https://github.com/aws-amplify/amplify-android/issues/1109))
- datastore(feat): support delete by model type with predicate ([#1106](https://github.com/aws-amplify/amplify-android/issues/1106))
- chore(devmenu) use java.util.Date instead of java.time for dev menu logging ([#1117](https://github.com/aws-amplify/amplify-android/issues/1117))
- feat(datastore) only include changed fields in update mutations ([#1110](https://github.com/aws-amplify/amplify-android/issues/1110))
- chore(core) increment SDK version to 2.22.0 ([#1118](https://github.com/aws-amplify/amplify-android/issues/1118))
- fix(datastore) fix crash caused by null patchItem ([#1123](https://github.com/aws-amplify/amplify-android/issues/1123))
- chore(datastore) minor simplification in SQLiteStorageAdapter ([#1120](https://github.com/aws-amplify/amplify-android/issues/1120))
- Make MatchAll/NoneQueryPredicate classes private ([#1127](https://github.com/aws-amplify/amplify-android/issues/1127))
- Release 1.6.10 ([#1124](https://github.com/aws-amplify/amplify-android/issues/1124))
- fix incorrectly serialized model for delete ([#1131](https://github.com/aws-amplify/amplify-android/issues/1131))
- chore(ci/cd):Generate build reports from DF data ([#1136](https://github.com/aws-amplify/amplify-android/issues/1136))
- feat(datastore) add support for notContains query operator ([#1145](https://github.com/aws-amplify/amplify-android/issues/1145))
- fix(datastore) include cause on error thrown when observing storage times out ([#1165](https://github.com/aws-amplify/amplify-android/issues/1165))
- chore(devmenu) set dev menu disabled by default ([#1167](https://github.com/aws-amplify/amplify-android/issues/1167))
- chore(ci) retry build 3 times before failing ([#1166](https://github.com/aws-amplify/amplify-android/issues/1166))
- release: 1.16.14 ([#1198](https://github.com/aws-amplify/amplify-android/issues/1198))
- ci: fix maven publishing ([#1200](https://github.com/aws-amplify/amplify-android/issues/1200))
- release: 1.16.15 ([#1201](https://github.com/aws-amplify/amplify-android/issues/1201))
- release: Core kotlin 0.1.1 ([#1202](https://github.com/aws-amplify/amplify-android/issues/1202))
- release: 1.17.0 ([#1213](https://github.com/aws-amplify/amplify-android/issues/1213))
- fix(rest-api) Expose HTTP headers in RestResponse ([#1184](https://github.com/aws-amplify/amplify-android/issues/1184))
- fix(datastore) improve storage adapter performance ([#1161](https://github.com/aws-amplify/amplify-android/issues/1161))
- chore(datastore) ignore OperationDisabled errors in SubscriptionProcessor ([#1209](https://github.com/aws-amplify/amplify-android/issues/1209))
- chore(release) Release v1.17.1 ([#1239](https://github.com/aws-amplify/amplify-android/issues/1239))
- feat(api):allows callers to specify auth mode ([#1238](https://github.com/aws-amplify/amplify-android/issues/1238))
- fix(datastore) Make PendingMutationConverter work for SerializedModel ([#1253](https://github.com/aws-amplify/amplify-android/issues/1253))
- release: Amplify Android 1.17.3 ([#1285](https://github.com/aws-amplify/amplify-android/issues/1285))
- depend on latest sdk version ([#1294](https://github.com/aws-amplify/amplify-android/issues/1294))
- Upgrade dependency on the latest SDK version ([#1296](https://github.com/aws-amplify/amplify-android/issues/1296))
- release: Amplify Android 1.17.4 ([#1299](https://github.com/aws-amplify/amplify-android/issues/1299))
- release: Amplify Android 1.17.5 ([#1305](https://github.com/aws-amplify/amplify-android/issues/1305))
- release: Amplify Android 1.17.6 ([#1336](https://github.com/aws-amplify/amplify-android/issues/1336))
- create stale bot GitHub action ([#1337](https://github.com/aws-amplify/amplify-android/issues/1337))
- refactor:add enum to represent auth rule provider ([#1320](https://github.com/aws-amplify/amplify-android/issues/1320))
- default to mobile client for iam auth mode ([#1351](https://github.com/aws-amplify/amplify-android/issues/1351))
- release: Amplify Android 1.17.7 ([#1354](https://github.com/aws-amplify/amplify-android/issues/1354))
- release: Amplify Android 1.17.8 ([#1367](https://github.com/aws-amplify/amplify-android/issues/1367))
- release: Amplify Android 1.18.0 ([#1372](https://github.com/aws-amplify/amplify-android/issues/1372))
- chore(api):tweaks to the api init process ([#1309](https://github.com/aws-amplify/amplify-android/issues/1309))
- Update stale.yml ([#1380](https://github.com/aws-amplify/amplify-android/issues/1380))
- release: Amplify Android 1.19.0 ([#1382](https://github.com/aws-amplify/amplify-android/issues/1382))
- Update SDK version to 2.26.0 ([#1386](https://github.com/aws-amplify/amplify-android/issues/1386))
- release: Amplify Android 1.20.0 ([#1387](https://github.com/aws-amplify/amplify-android/issues/1387))
- release: Amplify Android 1.20.1 ([#1400](https://github.com/aws-amplify/amplify-android/issues/1400))
- release: Amplify Android 1.21.0 ([#1406](https://github.com/aws-amplify/amplify-android/issues/1406))
- Update stale.yml ([#1404](https://github.com/aws-amplify/amplify-android/issues/1404))
- chore:fix dependabot alert for addressable gem ([#1410](https://github.com/aws-amplify/amplify-android/issues/1410))
- release: Amplify Android 1.22.0 ([#1418](https://github.com/aws-amplify/amplify-android/issues/1418))
- Delete stale.yml ([#1421](https://github.com/aws-amplify/amplify-android/issues/1421))
- Updated DataStore delete test based on expected delete behavior ([#1423](https://github.com/aws-amplify/amplify-android/issues/1423))
- feat(api) add CUSTOM case to AuthStrategy ([#1428](https://github.com/aws-amplify/amplify-android/issues/1428))
- release: Amplify Android 1.23.0 ([#1433](https://github.com/aws-amplify/amplify-android/issues/1433))
- release: Amplify Android 1.24.0 ([#1445](https://github.com/aws-amplify/amplify-android/issues/1445))
- release: Amplify Android 1.24.1 ([#1457](https://github.com/aws-amplify/amplify-android/issues/1457))
- release: Amplify Android 1.25.0 ([#1467](https://github.com/aws-amplify/amplify-android/issues/1467))
- fix(predictions):remove invalid test ([#1476](https://github.com/aws-amplify/amplify-android/issues/1476))
- release: Amplify Android 1.25.1 ([#1477](https://github.com/aws-amplify/amplify-android/issues/1477))
- release: Amplify Android 1.26.0 ([#1492](https://github.com/aws-amplify/amplify-android/issues/1492))
- release: Amplify Android 1.27.0 ([#1508](https://github.com/aws-amplify/amplify-android/issues/1508))
- Fix for issue with foreign keys on schema upgrade delete ([#1501](https://github.com/aws-amplify/amplify-android/issues/1501))
- better announce which schema is failing to sync ([#1479](https://github.com/aws-amplify/amplify-android/issues/1479))
- Observe query ([#1470](https://github.com/aws-amplify/amplify-android/issues/1470))
- release: Amplify Android 1.28.0 ([#1517](https://github.com/aws-amplify/amplify-android/issues/1517))
- Observe query updates ([#1520](https://github.com/aws-amplify/amplify-android/issues/1520))
- Update AWS SDK ver to 2.33.0 ([#1526](https://github.com/aws-amplify/amplify-android/issues/1526))
- release: Amplify Android 1.28.1 ([#1528](https://github.com/aws-amplify/amplify-android/issues/1528))
- fix(datastore):predicate handling for observe ([#1537](https://github.com/aws-amplify/amplify-android/issues/1537))
- release: Amplify Android 1.28.2 ([#1539](https://github.com/aws-amplify/amplify-android/issues/1539))
- Update build.gradle ([#1553](https://github.com/aws-amplify/amplify-android/issues/1553))
- release: Amplify Android 1.28.3 ([#1560](https://github.com/aws-amplify/amplify-android/issues/1560))
- Send snapshot on updates which render an item in list to not qualify a predicate.
- Revert "Send snapshot on updates which render an item in list to not qualify a predicate."
- release: Amplify Android 1.29.0 ([#1569](https://github.com/aws-amplify/amplify-android/issues/1569))
- Update build.gradle ([#1578](https://github.com/aws-amplify/amplify-android/issues/1578))
- release: Amplify Android 1.29.1 ([#1582](https://github.com/aws-amplify/amplify-android/issues/1582))
- Oq update ([#1567](https://github.com/aws-amplify/amplify-android/issues/1567))
- release: Amplify Android 1.30.0 ([#1586](https://github.com/aws-amplify/amplify-android/issues/1586))
- Adding create composite primary key on sqllite table ([#1590](https://github.com/aws-amplify/amplify-android/issues/1590))
- release: Amplify Android 1.30.1 ([#1593](https://github.com/aws-amplify/amplify-android/issues/1593))
- release: Amplify Android 1.31.0 ([#1600](https://github.com/aws-amplify/amplify-android/issues/1600))
- adjust pop-up elevation ([#1601](https://github.com/aws-amplify/amplify-android/issues/1601))
- Bump SDK version in build.gradle ([#1619](https://github.com/aws-amplify/amplify-android/issues/1619))
- release: Amplify Android 1.31.1 ([#1620](https://github.com/aws-amplify/amplify-android/issues/1620))
- release: Amplify Android 1.31.2 ([#1630](https://github.com/aws-amplify/amplify-android/issues/1630))
- Update build.gradle ([#1644](https://github.com/aws-amplify/amplify-android/issues/1644))
- Adding Dokka to the core-kotlin module ([#1645](https://github.com/aws-amplify/amplify-android/issues/1645))
- Adding support for custom primary key across the codebase.
- Adding support for custom primary key across the codebase.
- Added default implementation for resolveIdentifier in Model interface, to make it backwards compatible.
- Update build.gradle ([#1652](https://github.com/aws-amplify/amplify-android/issues/1652))
- release: Amplify Android 1.31.3 ([#1653](https://github.com/aws-amplify/amplify-android/issues/1653))
- Added default implementation for resolveIdentifier in Model interface, to make it backwards compatible.
- Added default implementation for resolveIdentifier in Model interface, to make it backwards compatible.
- Update notify_comments.yml ([#1654](https://github.com/aws-amplify/amplify-android/issues/1654))
- Updating the AWS SDK to 2.41.0 ([#1662](https://github.com/aws-amplify/amplify-android/issues/1662))
- release: Amplify Android 1.32.0 ([#1663](https://github.com/aws-amplify/amplify-android/issues/1663))
- Remove timeout for hydrating sync processor in orchestrator. ([#1658](https://github.com/aws-amplify/amplify-android/issues/1658))
- Update notify_comments.yml ([#1671](https://github.com/aws-amplify/amplify-android/issues/1671))
- release: Amplify Android 1.32.1 ([#1674](https://github.com/aws-amplify/amplify-android/issues/1674))
- Update notify_comments.yml ([#1675](https://github.com/aws-amplify/amplify-android/issues/1675))
- Remove the UUID restriction from persistentRecord ([#1678](https://github.com/aws-amplify/amplify-android/issues/1678))
- conflict resolver retry local fix ([#1634](https://github.com/aws-amplify/amplify-android/issues/1634))
- release: Amplify Android 1.32.2 ([#1682](https://github.com/aws-amplify/amplify-android/issues/1682))
- Work in progress Custom primary key in storage engine.
- Work in progress Custom primary key in storage engine.
- release: Amplify Android 1.33.0 ([#1687](https://github.com/aws-amplify/amplify-android/issues/1687))
- Connectivity crash fix ([#1688](https://github.com/aws-amplify/amplify-android/issues/1688))
- test fixes and check style.
- [aws-api] Fix DELETE rest calls not working with IamRequestDecorator ([#1684](https://github.com/aws-amplify/amplify-android/issues/1684))
- release: Amplify Android 1.34.0 ([#1691](https://github.com/aws-amplify/amplify-android/issues/1691))
- updated the pull request template to include information about tests and documentation update ([#1695](https://github.com/aws-amplify/amplify-android/issues/1695))
- Adding a new feature request template to our repo ([#1696](https://github.com/aws-amplify/amplify-android/issues/1696))
- Adding custom primary key support to SerializedModel.
- Updating selection set of leaf element to get primary key fields from the schema.
- Updating version of aws sdk ([#1698](https://github.com/aws-amplify/amplify-android/issues/1698))
- release: Amplify Android 1.35.0 ([#1699](https://github.com/aws-amplify/amplify-android/issues/1699))
- Added __typename to appsync selection set so the type can be computed for flutter serialized models.
- Conflict resolver fixes in comments. ([#1681](https://github.com/aws-amplify/amplify-android/issues/1681))
- Fixed integration test.
- Provide default message for GraphQLResponse.Error when null/missing ([#1700](https://github.com/aws-amplify/amplify-android/issues/1700))
- Fixed sql index on undefined index not getting created.
- Fixed sql index on undefined index not getting created.
- Reverting not needed change.
- Clean up and updated concatenation logic for primary key.
- Making SerializedModel resolve identifier return Serializable.
- Checkstyle fixes.
- Updated concatenation code for model primary key.
- Test fix.
- Test fix.
- Test fix.
- Test fix.
- Test fix.
- release: Amplify Android 1.35.1 ([#1705](https://github.com/aws-amplify/amplify-android/issues/1705))
- Optimization to make @@primary key only in case of composite primary key.
- Clean up
- Increasing timeout for flaky test.
- Ignoring the test failing in the build.
- Increasing timeout on AWSDatastore plugin for tests.
- Updating where.identifier method.
- Increasing timeout for slow running test.
- Update testmodels/src/main/java/com/amplifyframework/testmodels/personcar/PersonWithCPK.java
- Update aws-api-appsync/src/main/java/com/amplifyframework/datastore/appsync/ModelMetadata.java
- Update aws-api-appsync/src/main/java/com/amplifyframework/datastore/appsync/ModelMetadata.java
- Update aws-api-appsync/src/main/java/com/amplifyframework/datastore/appsync/ModelMetadata.java
- Code review suggestions.
- Update aws-datastore/src/main/java/com/amplifyframework/datastore/storage/sqlite/adapter/SQLiteColumn.java
- Update core/src/main/java/com/amplifyframework/core/model/ModelPrimaryKey.java
- Update aws-datastore/src/androidTest/java/com/amplifyframework/datastore/BasicCloudSyncInstrumentationTest.java
- Code review suggestions.
- Increasing time out for orchestrator semaphore
- Update aws-datastore/src/main/java/com/amplifyframework/datastore/storage/sqlite/SqlQueryProcessor.java
- Formatting fixes.
- PR suggestions.
- Update core/src/main/java/com/amplifyframework/core/model/ModelPrimaryKey.java
- release: Amplify Android 1.35.2 ([#1709](https://github.com/aws-amplify/amplify-android/issues/1709))
- Update core/src/main/java/com/amplifyframework/core/model/ModelPrimaryKey.java
- Deprecating where.id method.
- Checkstyle
- where.id deprecation in datastore integration tests.
- Code optimization to create SQL index for primary key fields only if it is a composite.
- Fixed flaky test.
- Added an integration test for a flaky unit test.
- Bump version to 1.36.0-dev-preview.0 ([#1717](https://github.com/aws-amplify/amplify-android/issues/1717))
- Refactored the code around sync expression predicate of QueryPredicates.all and QueryPredicates.none().
- Checkstyle
- Removing integration test for custom primary untill I find a way to fix the environment.
- Version bumps ([#1721](https://github.com/aws-amplify/amplify-android/issues/1721))
- Accidental change.
- Indentations.
- Update changelog after manual release v1.35.3 ([#1724](https://github.com/aws-amplify/amplify-android/issues/1724))
- Fixing get unique key function on Model primary key helper.
- Update aws-datastore/src/main/java/com/amplifyframework/datastore/storage/sqlite/adapter/SQLiteTable.java
- Fixing cascade delete on a child with custom primary key. ([#1731](https://github.com/aws-amplify/amplify-android/issues/1731))
- Updated schema version of testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql to the latest version.
- Modifying create mutation to handle custom foreign keys.
- Updating Create mutation to work with custom foreign keys.
- Update SDK version in build.gradle ([#1741](https://github.com/aws-amplify/amplify-android/issues/1741))
- release: Amplify Android 1.35.4 ([#1742](https://github.com/aws-amplify/amplify-android/issues/1742))
- Updating Create mutation to work with custom foreign keys.
- Fix format errors
- Updating Create mutation to work with custom foreign keys.
- Fix unit tests
- Fix format errors
- Fixing cascade delete on a child with custom primary key. ([#1731](https://github.com/aws-amplify/amplify-android/issues/1731))
- Update notify_comments.yml ([#1746](https://github.com/aws-amplify/amplify-android/issues/1746))
- Update SDK version in build.gradle ([#1747](https://github.com/aws-amplify/amplify-android/issues/1747))
- release: Amplify Android 1.35.5 ([#1748](https://github.com/aws-amplify/amplify-android/issues/1748))
- Fix typos
- Fix typos
- add comment
- Updating custom key models and adding a unit test with  no sort keys
- Updated comment models.   Added unit test for custom key with no  sort key.
- Updated tests and db models to match new schema version.  Added a test for model with a custoom primary key and no sort keys.
- Cpk bidirectional ([#1751](https://github.com/aws-amplify/amplify-android/issues/1751))
- Fix create mutations for custom PK ([#1740](https://github.com/aws-amplify/amplify-android/issues/1740))
- Added logic to delete with class name and predicate to support custom primary key.
- Revert "Added logic to delete with class name and predicate to support custom primary key."
- Dev preview update and version bump ([#1752](https://github.com/aws-amplify/amplify-android/issues/1752))
- Delete with class and predicate as parameters support for custom primary key ([#1760](https://github.com/aws-amplify/amplify-android/issues/1760))
- Get pk ([#1761](https://github.com/aws-amplify/amplify-android/issues/1761))
- Appsync mutation fk ([#1762](https://github.com/aws-amplify/amplify-android/issues/1762))
- Fixing outbox event processed, ([#1765](https://github.com/aws-amplify/amplify-android/issues/1765))
- ignore flaky test ([#1768](https://github.com/aws-amplify/amplify-android/issues/1768))
- upgrade jmespath to version 1.6.1 ([#1766](https://github.com/aws-amplify/amplify-android/issues/1766))
- Create closed_issue_message.yml ([#1754](https://github.com/aws-amplify/amplify-android/issues/1754))
- Bump SDK version to 2.48.0 ([#1773](https://github.com/aws-amplify/amplify-android/issues/1773))
- release version bump ([#1774](https://github.com/aws-amplify/amplify-android/issues/1774))
- Fix for adding data back to the delete request if it contains data as that is allowed for the Delete api ([#1735](https://github.com/aws-amplify/amplify-android/issues/1735))
- Fix/1485 : Fix for sending the session expired hub event when all credentials are expired ([#1779](https://github.com/aws-amplify/amplify-android/issues/1779))
- Updating build.gradle to include the latest version of the aws sdk ([#1783](https://github.com/aws-amplify/amplify-android/issues/1783))
- release: Amplify Android 1.35.7 ([#1784](https://github.com/aws-amplify/amplify-android/issues/1784))
- merge main into dev-preview ([#1777](https://github.com/aws-amplify/amplify-android/issues/1777))
- added implementation for updatepassword
- Refactor Geo category to use AWS Kotlin SDKs Location. ([#1771](https://github.com/aws-amplify/amplify-android/issues/1771))
- cleanup dependencies and fix auth unit test ([#1787](https://github.com/aws-amplify/amplify-android/issues/1787))
- Suppress belongsto index ([#1789](https://github.com/aws-amplify/amplify-android/issues/1789))
- Update build.gradle
- release: Amplify Android 1.36.0 ([#1796](https://github.com/aws-amplify/amplify-android/issues/1796))
- fix orchestrator failing if emitter is disposed ([#1755](https://github.com/aws-amplify/amplify-android/issues/1755))
- catch exceptions from processOutboxItem ([#1743](https://github.com/aws-amplify/amplify-android/issues/1743))
- ci: added canary workflow ([#1770](https://github.com/aws-amplify/amplify-android/issues/1770))
- release: Amplify Android 1.36.1
- Expand a catch clause to catch all ([#1806](https://github.com/aws-amplify/amplify-android/issues/1806))
- release: Amplify Android 1.36.2 ([#1812](https://github.com/aws-amplify/amplify-android/issues/1812))
- added implementation for fetchUserAttribute
- Flutter fix ([#1769](https://github.com/aws-amplify/amplify-android/issues/1769))
- Call to start TransferService regardless of if it has already been started
- Integration test fix ([#1815](https://github.com/aws-amplify/amplify-android/issues/1815))
- Checkstyle fixes.
- added implementation for fetchUserAttribute
- Integration test fix ([#1820](https://github.com/aws-amplify/amplify-android/issues/1820))
- Checkstyle fixes
- Increasing timeout for test.
- Ignoring flaky test.
- Integration test ([#1821](https://github.com/aws-amplify/amplify-android/issues/1821))
- feat(auth):Make Current user an Asynchronous call (Breaking change) ([#1807](https://github.com/aws-amplify/amplify-android/issues/1807))
- Version update ([#1825](https://github.com/aws-amplify/amplify-android/issues/1825))
- Cpk version update ([#1826](https://github.com/aws-amplify/amplify-android/issues/1826))
- added implementation for fetchUserAttribute and updateUserAttributes
- Fixing flutter sql predicate issue Version update. ([#1832](https://github.com/aws-amplify/amplify-android/issues/1832))
- Flutter update ([#1833](https://github.com/aws-amplify/amplify-android/issues/1833))
- Refactor auth plugin for easier testing
- lint fixes
- lint fixes
- version update ([#1837](https://github.com/aws-amplify/amplify-android/issues/1837))
- add SignInState to handle other auth scenarios ([#1824](https://github.com/aws-amplify/amplify-android/issues/1824))
- update signIn method after merge
- update after merge
- release: Amplify Android 1.36.3 ([#1839](https://github.com/aws-amplify/amplify-android/issues/1839))
- added implementation for fetchUserAttribute,ConfirmUserAttribute and updateUserAttributes
- Update transfer message in notification
- Update the Delete REST API to ensure it would work with and without a body ([#1842](https://github.com/aws-amplify/amplify-android/issues/1842))
- added implementation for updatePassword api
- added implementation for updatePassword api
- resolved merge conflict
- resolved merge conflict
- resolved merge conflict
- resolved codebuild issue
- resolved codebuild issue
- Chore(Release): Updating mobile client to the latest ([#1847](https://github.com/aws-amplify/amplify-android/issues/1847))
- added fetchUserAttribute implementation
- release: Amplify Android 1.36.4 ([#1856](https://github.com/aws-amplify/amplify-android/issues/1856))
- added fetchUserAttribute implementation
- added fetchUserAttribute implementation and unit tests
- removed unnecessary file
- merged master
- merged master
- added message in exception
- added message in exception
- added message in exception
- Updating the version to 2.50.1 for AWS SDK ([#1861](https://github.com/aws-amplify/amplify-android/issues/1861))
- Reduce the importance level of transfer channel for foreground service to prevent sound/vibrate ([#1860](https://github.com/aws-amplify/amplify-android/issues/1860))
- release: Amplify Android 1.36.5 ([#1863](https://github.com/aws-amplify/amplify-android/issues/1863))
- added updateuserattributes implementation
- changed timeout to countdownlatch
- Fix for flutter backtick
- Update aws-datastore/src/main/java/com/amplifyframework/datastore/storage/sqlite/SQLiteCommandFactory.java
- version update for flutter backtick
- changed timeout to countdownlatch
- added verify test to test cases
- release: Amplify Android 1.36.6
- update version numbers and removed miscellaneous section in changelog.md
- update version numbers and removed miscellaneous section in changelog.md
- update version numbers and removed miscellaneous section in changelog.md
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
- resolved merge conflict
- release: Amplify Android 1.37.1 ([#1881](https://github.com/aws-amplify/amplify-android/issues/1881))
- Update gradle.properties
- remove multiple plugin configures to fix auth state listeners ([#1872](https://github.com/aws-amplify/amplify-android/issues/1872))
- Run more devices in farm
- Update region
- change device filters
- Removing unused code.
- added test cases to updateuserattributes
- added implementation for confirm user attribute
- updated latch timeout to 5s
- removed pid from test class
- moved copyright text to top of page
- release: Amplify Android 1.37.2
- Update changelog
- added implementation for resenduserattributeconfirmationcode
- Fixing test failure because of list ordering.
- namespace test report for multiple devices
- fixed code review comments
- fixed code review comments
- Update generate_df_testrun_report
- lower polling on device farm
- fixed code review comments
- fixed merge conflict
- fixed review comment
- Adding retry ability to mutation processor
- fixed review comment
- Checkstyle fixes
- fixed review comment
- fixed review comment
- fixed review comment
- fixed review comment
- Publish Javadocs (Amplify version) ([#1897](https://github.com/aws-amplify/amplify-android/issues/1897))
- Checkstyle fixes
- added unit tests to resenduserattribute confirmation code
- dev-preview related changes
- Manual release: Amplify Android 1.37.3 ([#1924](https://github.com/aws-amplify/amplify-android/issues/1924))
- resolve ktlint exceeded line characters limit
- fixed code review comment
- @Ignore import got lost via merge
- DS flacky test fix.
- Update SignOut Flow ([#1915](https://github.com/aws-amplify/amplify-android/issues/1915))
- Unignore test since @poojamat merged timeout raise
- Lint unused import
- SignIn call when already signed in should not always call onSuccess
- Remove unused web options
- addressing PR comments
- ktlintFormat
- Remove notification client; attributes unnecessary
- Fix typo
- Call signout before calling signin
- ktlint
- ktlint format
- Cleaning up after sign in only when signin is called, call signout
- If signed out state then complete instead of proceeding with a signout
- Fixed tests and ignored one test to be fixed later
- continue signing out in error state
- revert
- fix tests
- (chore): Migrate Pinpoint tests from SDK and add others ([#1948](https://github.com/aws-amplify/amplify-android/issues/1948))
- update sign out flow and fix map tests
- lint
- change exception
- Update build.gradle ([#1959](https://github.com/aws-amplify/amplify-android/issues/1959))
- Fix unit tests after rebase due to notification client stub removal
- chore : set max devices to 1 for dev-preview ([#1972](https://github.com/aws-amplify/amplify-android/issues/1972))
- release: Amplify Android 1.37.4 ([#1969](https://github.com/aws-amplify/amplify-android/issues/1969))
- Feat(Auth): Implementation of Custom Auth with SRP ([#1976](https://github.com/aws-amplify/amplify-android/issues/1976))
- added open id attributes ([#1984](https://github.com/aws-amplify/amplify-android/issues/1984))
- Update build.gradle ([#1991](https://github.com/aws-amplify/amplify-android/issues/1991))
- release: Amplify Android 1.37.5 ([#1992](https://github.com/aws-amplify/amplify-android/issues/1992))
- Fix(Auth): Refactor devicehelper to keep what is required for cryptoBR ([#1993](https://github.com/aws-amplify/amplify-android/issues/1993))
- Add escape hatch and corresponding integ tests
- Make sure users are opted out of notifications since they're not supported (escape hatch method possible for customers, needs to call `updateEndpoint` after `identifyUser` but that was necessary anyway)
- Fix test from merge
- Remove unused import
- add sign in support w/o identity pool ([#2000](https://github.com/aws-amplify/amplify-android/issues/2000))
- Hosted UI Exception updates
- exception updates
- Log for integration testing on devicefarm
- Update exception name
- Fix session start/stop events giving error `Session start and stop timestamp's exist but duration is null`
- add user context data for cognito ASF ([#2005](https://github.com/aws-amplify/amplify-android/issues/2005))
- Prevent maplibre crash
- fix error message
- lint
- Fix pinpoint tests
- lint
- temporarily revert fix
- change federation parameter ordering
- Federated session fix
- Pull SDK name from user agent code instead of hardcode
- Fix (Auth): Remove deviceKey stub and implemented Device Credential Store ([#2002](https://github.com/aws-amplify/amplify-android/issues/2002))
- Fix federation start logic
- bugfix device srp ([#2018](https://github.com/aws-amplify/amplify-android/issues/2018))
- Fix session end submission error also
- fix sign in with MFA ([#2023](https://github.com/aws-amplify/amplify-android/issues/2023))
- Add clientMetadata to signIn
- fix tests
- fetch auth session fix
- Fix(Auth): Fix for error propagation when sign in fails inside Sign In Custom Auth Action ([#2022](https://github.com/aws-amplify/amplify-android/issues/2022))
- fix device key param for refresh tokens and custom auth ([#2030](https://github.com/aws-amplify/amplify-android/issues/2030))
- Fix(Auth): Fix for when device SRP does not throw a cancel sign in error ([#2027](https://github.com/aws-amplify/amplify-android/issues/2027))
- Wait for Pinpoint endpoint to update before fetching it -- should fix tests
- Fixing custom auth with srp ([#2031](https://github.com/aws-amplify/amplify-android/issues/2031))
- Get new endpoint and user on test runs
- Fix(Auth): Fix for when we want to delete the user ([#2034](https://github.com/aws-amplify/amplify-android/issues/2034))
- FIx(Auth): Fix fetch attributes return type to return list instead of mutable list ([#2039](https://github.com/aws-amplify/amplify-android/issues/2039))
- removed the TODO as this is now done ([#2040](https://github.com/aws-amplify/amplify-android/issues/2040))
- Dokka multi module support
- Lower visibility levels
- Update build.gradle ([#2045](https://github.com/aws-amplify/amplify-android/issues/2045))
- move auth featuretest
- Auth hub events fired from api calls instead of SM ([#2051](https://github.com/aws-amplify/amplify-android/issues/2051))
- Local sign out test
- lint
- don't require options on parameters
- Feat(Auth): Delete user test case generator ([#2057](https://github.com/aws-amplify/amplify-android/issues/2057))
- Fix(Auth): Fix for updating device metadata test on legacy credential store ([#2044](https://github.com/aws-amplify/amplify-android/issues/2044))
- merge
- Hosted UI parse token fix
- Unknown migration path
- lint
- add confirm sign in test generator ([#2065](https://github.com/aws-amplify/amplify-android/issues/2065))
- Add HostedUI error that customer can retry sign out
- release: Amplify Android 1.37.6 ([#2068](https://github.com/aws-amplify/amplify-android/issues/2068))
- Chore(Auth) - Add Device srp test case ([#2064](https://github.com/aws-amplify/amplify-android/issues/2064))
- lint
- Ensure browser package is set and hosted ui error is passed
- replace jcenter with mavenCentral
- Track pinpoint endpoint id if set for auth
- replace jcenter with mavenCentral
- FlutterFactory
- Pinpoint changes
- lint
- remove non-working dokka suppression
- Update code comment
- attempt to fix tests
- update test
- lint
- update code comment
- Fix for deleting all directories before creating them ([#2067](https://github.com/aws-amplify/amplify-android/issues/2067))
- Reduce Analytics visibilty ([#2083](https://github.com/aws-amplify/amplify-android/issues/2083))
- Fix for invalid state exception when the user is signed out ([#2097](https://github.com/aws-amplify/amplify-android/issues/2097))
- Update release_pr.yml ([#2105](https://github.com/aws-amplify/amplify-android/issues/2105))
- Prevent attempting to read backed up EncryptedSharedPreferences that are no longer readable ([#2113](https://github.com/aws-amplify/amplify-android/issues/2113))
- Number of attributes being too high is not retryable ([#2112](https://github.com/aws-amplify/amplify-android/issues/2112))
- Change errors returned on some apis while federated ([#2116](https://github.com/aws-amplify/amplify-android/issues/2116))
- release: Amplify Android 2.0.0 ([#2115](https://github.com/aws-amplify/amplify-android/issues/2115))
- Fix for when move to idle state is called twice ([#2152](https://github.com/aws-amplify/amplify-android/issues/2152))
- Update README.md ([#2120](https://github.com/aws-amplify/amplify-android/issues/2120))
- Dengdan stress test ([#2153](https://github.com/aws-amplify/amplify-android/issues/2153))
- Fix(Auth): Sign up if successful should return DONE instead of Confirm sign up ([#2130](https://github.com/aws-amplify/amplify-android/issues/2130))
- Feat(Auth Test): Custom party testing for Custom Test without SRP ([#2149](https://github.com/aws-amplify/amplify-android/issues/2149))
- Unignore storage and pinpoint tests ([#2156](https://github.com/aws-amplify/amplify-android/issues/2156))
- Update DeviceFarm build config ([#2168](https://github.com/aws-amplify/amplify-android/issues/2168))
- Add Geo Rx Bindings ([#2159](https://github.com/aws-amplify/amplify-android/issues/2159))
- Add a network status listener to restart DataStore after the network … ([#2148](https://github.com/aws-amplify/amplify-android/issues/2148))
- Add a buildspec file for nightly tests ([#2180](https://github.com/aws-amplify/amplify-android/issues/2180))
- Chore(Auth): Implementation of the custom auth with SRP parity testing use case ([#2167](https://github.com/aws-amplify/amplify-android/issues/2167))
- release: Amplify Android 2.1.0 (manually created) ([#2185](https://github.com/aws-amplify/amplify-android/issues/2185))
- release: Amplify Android 2.1.1 ([#2257](https://github.com/aws-amplify/amplify-android/issues/2257))
- release: Amplify Android 2.2.0 ([#2269](https://github.com/aws-amplify/amplify-android/issues/2269))
- release: Amplify Android 2.2.1 ([#2285](https://github.com/aws-amplify/amplify-android/issues/2285))
- release: Amplify Android 2.2.2 ([#2292](https://github.com/aws-amplify/amplify-android/issues/2292))

[See all changes between 1.38.1 and 1.39.0](https://github.com/aws-amplify/amplify-android/compare/release_v1.38.1...release_v1.39.0)

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
