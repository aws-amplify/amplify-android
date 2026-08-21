# CloudWatch Client E2E Test Infrastructure

Instructions for deploying the backend used by the `aws-cloudwatch` integration tests
(`AmplifyCloudWatchClientInstrumentationTest`).

The infrastructure is not committed to the repo — deploy it from a scratch folder using the snippets
below. The tests obtain **guest (unauthenticated)** AWS credentials from the provisioned Cognito
identity pool, so no test user is required.

## Part 1: Deploy the Backend

From a new folder outside this repo, run `npm create amplify@latest`, then replace the generated files
with the content below.

**amplify/auth/resource.ts**

```ts
import { defineAuth } from '@aws-amplify/backend';

/**
 * Define and configure your auth resource
 * @see https://docs.amplify.aws/gen2/build-a-backend/auth
 */
export const auth = defineAuth({
  loginWith: {
    email: true,
  },
});
```

**amplify/custom/LoggingConstruct/resource.ts**

```ts
import * as cdk from "aws-cdk-lib"
import { Construct } from "constructs"
import * as logs from "aws-cdk-lib/aws-logs"
import * as iam from "aws-cdk-lib/aws-iam"

export class LoggingConstruct extends Construct {
  constructor(scope: Construct, id: string, authRoleName: string, unAuthRoleName: string) {
    super(scope, id)

    const region = cdk.Stack.of(this).region
    const account = cdk.Stack.of(this).account
    const logGroupName = "cloudwatch-integration-test-log-group"

    new logs.LogGroup(this, 'Log Group', {
      logGroupName: logGroupName,
      retention: logs.RetentionDays.INFINITE
    })

    const authRole = iam.Role.fromRoleName(this, "Auth-Role", authRoleName)
    const unAuthRole = iam.Role.fromRoleName(this, "UnAuth-Role", unAuthRoleName)
    const logResource = `arn:aws:logs:${region}:${account}:log-group:${logGroupName}:log-stream:*`
    const logIAMPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      resources: [logResource],
      actions: ["logs:PutLogEvents", "logs:DescribeLogStreams", "logs:CreateLogStream", "logs:FilterLogEvents"]
    })

    authRole.addToPrincipalPolicy(logIAMPolicy)
    unAuthRole.addToPrincipalPolicy(logIAMPolicy)

    new cdk.CfnOutput(this, 'CloudWatchLogGroupName', { value: logGroupName });
    new cdk.CfnOutput(this, 'CloudWatchRegion', { value: region });
  }
}
```

> The test verifies delivery with `FilterLogEvents`, so the policy grants `logs:FilterLogEvents` in
> addition to the put/describe/create actions the client uses.

**amplify/backend.ts**

```ts
import { defineBackend } from '@aws-amplify/backend';
import { auth } from './auth/resource';
import { LoggingConstruct } from './custom/LoggingConstruct/resource';

const backend = defineBackend({
  auth,
});

// Auth - sign in with username
const { cfnUserPool } = backend.auth.resources.cfnResources
cfnUserPool.usernameAttributes = []

// ============ Logging Stack ===========

new LoggingConstruct(
  backend.createStack('logging-stack'),
  'logging-stack',
  backend.auth.resources.authenticatedUserIamRole.roleName,
  backend.auth.resources.unauthenticatedUserIamRole.roleName
);
```

Deploy with Amplify sandbox:

```bash
npx ampx sandbox --profile [YOUR_AWS_PROFILE]
```

This creates:
- Cognito User Pool + Identity Pool (guest access enabled)
- CloudWatch log group `cloudwatch-integration-test-log-group`
- IAM policies granting both the authenticated and unauthenticated roles the CloudWatch Logs actions
  the tests need

Deployment generates an `amplify_outputs.json` file.

## Part 2: Copy Configuration Files

The tests load two files from `aws-cloudwatch/src/androidTest/res/raw/` (both untracked — supply them
at test time):

1. Copy the generated Auth config into place:

```bash
cp amplify_outputs.json \
  aws-cloudwatch/src/androidTest/res/raw/amplify_outputs.json
```

2. Create the client configuration file. Set `region` to where the backend was deployed:

```bash
cat > aws-cloudwatch/src/androidTest/res/raw/amplifyconfiguration_logging.json << 'EOF'
{
    "cloudWatchClient": {
        "region": "<your-region>",
        "logGroupName": "cloudwatch-integration-test-log-group",
        "localStoreMaxSizeInMB": 1,
        "flushIntervalInSeconds": 60,
        "loggingConstraints": {
            "defaultLogLevel": "VERBOSE"
        }
    }
}
EOF
```

## Part 3: Run the Tests

Run the instrumentation tests against a connected device or emulator:

```bash
./gradlew :aws-cloudwatch:connectedAndroidTest
```
