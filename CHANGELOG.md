# Release 1.17.3

### Bug Fixes
- **datastore:** optimize sync queries with predicates by wrapping in an AND group (#1225)
- **kotlin:** getCurrentUser return type should be nullable (#1265)
- **ci:** update branch name and PR subject (#1276)
- change protocol for github import (#1284)
- throws AlreadyConfiguredException when configured == true (#1274)

### 
- **datastore:** fix unit tests by adding createdAt updatedAt (#1266)
- manually triggered workflow to start release process (#1196)
- **build:** add buildspec for publishing to maven (#1271)
- small updates to the create_next_release_pr workflow (#1278)
- update release number in the README.md file (#1277)
- update name of workflow that prepares next release (#1280)
- bump sdk version to 2.22.6 (#1281)
- use shared lane file (#1282)

[See all changes between 1.17.2 and 1.17.3](https://github.com//compare/release_v1.17.2...release_v1.17.3)
