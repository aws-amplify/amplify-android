# Release 1.17.3

### Bug fixes
- **datastore:** optimize sync queries with predicates by wrapping in an AND group (#1225) ([f6a0c373](/f6a0c373b437f609385e8ac8ba00828a7cc8d48b))
- **kotlin:** getCurrentUser return type should be nullable (#1265) ([682c9e77](/682c9e778adac74c2261bce4de4866c5e255a5eb))

### Building system
- **datastore:** fix unit tests by adding createdAt updatedAt (#1266) ([2f759876](/2f759876bfc0ca1c812c84c55b973752d79a479c))
- manually triggered workflow to start release process (#1196) ([ed84e14e](/ed84e14efa4ab32f7a4b18d8d6a8803d6539406f))
- **build:** add buildspec for publishing to maven (#1271) ([a8ff1dd6](/a8ff1dd6b9def0ba331967528ba437c7c14e2295))

