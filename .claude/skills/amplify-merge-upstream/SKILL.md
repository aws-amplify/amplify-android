---
name: amplify-merge-upstream
description: Merge a new aws-amplify/amplify-android upstream release into this Harri fork while preserving multi-user auth changes. Use when bumping the fork to a new upstream tag (e.g. release_v2.36.0), resolving conflicts in RealAWSCognitoAuthPlugin / use cases / AuthCategoryBehavior, or auditing what diverges from upstream.
---

# Merging an upstream Amplify release into the fork

The fork's main long-lived divergence is multi-user auth. Most upstream releases conflict in a predictable set of files. This skill is the playbook.

## Remotes

```
origin     git@github.com:HarriLLC/amplify-android.git    (the fork)
upstream   git@github.com:aws-amplify/amplify-android.git (canonical Amplify)
```

If `upstream` is missing: `git remote add upstream git@github.com:aws-amplify/amplify-android.git`.

## Pre-flight checks (always)

```bash
git fetch upstream --tags
git fetch origin
git tag --list | sort -V | awk '/^release_v2\./' | tail -10   # latest upstream tags
git log --oneline release_v<current>..release_v<target>       # what's coming in
git status                                                    # working tree clean
```

Pick a target tag explicitly — never merge `upstream/main`. The release tags carry the version constants and `CHANGELOG.md` updates we want.

## Branching convention

`feature/upgrade-to-<target-version>-with-multi-user`. Example: `feature/upgrade-to-2.36.0-with-multi-user`.

Branch off the current fork tip (usually `main` or the prior upgrade branch):

```bash
git checkout -b feature/upgrade-to-2.36.0-with-multi-user origin/main
```

## Files that always conflict

Keep this list current as the fork evolves. As of the 2.34.0 upgrade these are the predictable conflict zones:

| File | Why it conflicts |
|---|---|
| `aws-auth-cognito/src/main/java/com/amplifyframework/auth/cognito/RealAWSCognitoAuthPlugin.kt` | Fork adds `userId` overloads; upstream is shrinking it into use cases |
| `aws-auth-cognito/.../usecases/SignOutUseCase.kt` | Fork adds `userId` parameter |
| `aws-auth-cognito/.../usecases/FetchAuthSessionUseCase.kt` | Fork adds `userId` parameter |
| `aws-auth-cognito/.../usecases/ClearFederationToIdentityPoolUseCase.kt` | Fork adds `userId` parameter |
| `aws-auth-cognito/.../statemachine/codegen/states/AuthorizationState.kt` / `DeleteUserState.kt` / `SignOutState.kt` | Fork carries small state shape changes |
| `core/src/main/java/com/amplifyframework/auth/AuthCategory.java` | Fork adds `userId` overloads |
| `core/src/main/java/com/amplifyframework/auth/AuthCategoryBehavior.java` | Fork adds `userId` overloads |
| `core/src/main/java/com/amplifyframework/auth/result/AuthSignInResult.java` | Fork adds extra fields |
| `core/src/main/java/com/amplifyframework/core/Amplify.java`, `Category.java`, `Plugin.java` | Fork extends Plugin lifecycle for multi-user |
| `core-kotlin/.../KotlinAuthFacade.kt` + test | Mirrors the Java surface |
| `rxbindings/.../RxAuthBinding.java`, `RxAuthCategoryBehavior.java` | Mirrors the Java surface |
| `testutils/.../sync/SynchronousAuth.java` | Mirrors the Java surface |
| `aws-auth-cognito/.../statemachine/util/ThreadSafeLifoMap.kt` | Fork-only file (no conflict, but verify it survives) |

When upstream adds a use case, expect the parameter list to differ from the fork's because the fork's already accepts `userId`.

## The merge

```bash
git merge release_v<target> --no-ff
# fix conflicts (see below)
./gradlew clean ktlintFormat                 # cosmetics
./gradlew build                              # compile
./gradlew :aws-auth-cognito:test :core:test  # the modules most affected
./gradlew apiCheck                           # public API drift
git commit
```

Do NOT use `--no-verify`. If a hook fails, fix the cause.

## Resolving conflicts — rules of thumb

1. **Multi-user wins for auth signatures.** When upstream removes a `userId` parameter or introduces a new method without one, keep the fork's `userId`-bearing version AND add a delegating overload that matches upstream's signature with `userId = null`.

2. **Upstream wins for state-machine behaviour.** Upstream resolvers / actions / state transitions are the source of truth. The fork should rarely touch resolver logic. If a conflict is "upstream changed a `when` branch and we have something different", upstream is almost always right — port the fork's userId threading on top of upstream's logic.

3. **Use cases are the integration point.** Upstream often refactors `RealAWSCognitoAuthPlugin` into a new use case between releases. When you see `chore(auth): Move <X> to usecase` in the changelog, the fork's hand-rolled implementation in `RealAWSCognitoAuthPlugin` should be removed and the new upstream use case adopted — but with `userId` plumbed through.

4. **Don't fight `AuthCategoryBehavior` shape changes.** When upstream adds a new method, add it AND its `userId` overload. When upstream changes a signature, update both.

5. **`apiDump` is your friend.** After resolving conflicts: `./gradlew apiDump`. Read the diff. Every line of public-API change should be either (a) something we added on purpose for multi-user, or (b) something upstream changed. Anything else is a merge mistake.

## After the merge

```bash
./gradlew ktlintCheck checkstyle apiCheck    # the gate
./gradlew :aws-auth-cognito:test             # auth tests
./gradlew test                               # everything else
./gradlew :foundation:test :core:test :core-kotlin:test    # core slice
```

Spot-check the changelog the merge introduces:

```bash
git log --no-merges --oneline release_v<previous>..release_v<target> > /tmp/changes.txt
grep -E '^[a-f0-9]+ (feat|fix)\(auth\)' /tmp/changes.txt    # auth-related
```

Every `feat(auth)` and `fix(auth)` from upstream needs a quick read-through to confirm the fork hasn't accidentally undone it.

## Auditing divergence (no merge)

Sometimes you just want to know "how far have we drifted?":

```bash
git diff release_v<latest-merged> -- aws-auth-cognito/ core/ core-kotlin/ rxbindings/ \
    | diffstat | tail -40
git log --no-merges --oneline release_v<latest-merged>..HEAD -- aws-auth-cognito/ core/
```

A healthy fork keeps the divergence diff stable across upgrades — it shouldn't grow over time. If it is, one of the multi-user changes hasn't been upstreamed and it should be a candidate for an upstream PR.

## Anti-patterns

- Merging `upstream/main`. Use the release tag.
- Resolving a conflict by deleting the fork's `userId` plumbing. That's a regression — escalate before doing it.
- Skipping `apiDump`. CI will catch it but the diff is much larger after another commit.
- Reformatting (ktlintFormat) and resolving conflicts in the same commit. Split: one commit for the merge, one for cosmetics. Reviewers will thank you.
- Bumping to a tag without reading its `CHANGELOG.md`. Two minutes of reading saves an hour of debugging.
