# Kinesis Data Streams E2E Test Infrastructure

Instructions for deploying the backend used by `aws-kinesis` integration tests.

The infrastructure is not committed to the repo — deploy it from a scratch folder using the snippets below.

## Part 1: Deploy the Backend

From a new folder outside this repo, run `npm create amplify@latest`, then replace the generated files with the content below.

**package.json**

```json
{
  "name": "kinesis-e2e-test-infra",
  "version": "1.0.0",
  "type": "module",
  "devDependencies": {
    "@aws-amplify/backend": "^1.21.0",
    "@aws-amplify/backend-cli": "^1.8.2",
    "aws-cdk-lib": "^2.234.1",
    "constructs": "^10.5.1",
    "esbuild": "^0.27.3",
    "tsx": "^4.21.0",
    "typescript": "^5.9.3"
  },
  "dependencies": {
    "aws-amplify": "^6.16.2"
  }
}
```

**amplify/auth/resource.ts**

```ts
import { defineAuth } from '@aws-amplify/backend';

export const auth = defineAuth({
  loginWith: {
    email: true,
  },
});
```

**amplify/backend.ts**

```ts
import { defineBackend } from '@aws-amplify/backend';
import { auth } from './auth/resource';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
import { Duration } from 'aws-cdk-lib';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';

const backend = defineBackend({
  auth,
});

const kinesisStack = backend.createStack('KinesisStack');

const stream = new kinesis.Stream(kinesisStack, 'TestStream', {
  streamName: 'amplify-kinesis-test-stream',
  shardCount: 1,
  retentionPeriod: Duration.hours(24),
});

// Grant authenticated users permission to put records
backend.auth.resources.authenticatedUserIamRole.addToPrincipalPolicy(
  new PolicyStatement({
    actions: [
      'kinesis:PutRecord',
      'kinesis:PutRecords',
      'kinesis:DescribeStream',
    ],
    resources: [stream.streamArn],
  })
);
```

Deploy with Amplify sandbox:

```bash
npx ampx sandbox --profile [YOUR_AWS_PROFILE]
```

This creates:
- Cognito User Pool + Identity Pool
- Kinesis Data Stream (`amplify-kinesis-test-stream`, 1 shard, 24h retention)
- IAM policy granting `kinesis:PutRecord`, `kinesis:PutRecords`, `kinesis:DescribeStream` to the authenticated role

## Part 2: Create a Test User

After the backend is deployed, create a user in the Cognito User Pool:

```bash
# Get the User Pool ID from amplify_outputs.json
USER_POOL_ID=$(cat amplify_outputs.json | python3 -c "import sys,json; print(json.load(sys.stdin)['auth']['user_pool_id'])")

# Create the user
aws cognito-idp admin-create-user \
  --user-pool-id $USER_POOL_ID \
  --username [EMAIL] \
  --temporary-password '[TEMP_PASSWORD]' \
  --user-attributes Name=email,Value=[EMAIL] Name=email_verified,Value=true \
  --message-action SUPPRESS \
  --profile [YOUR_AWS_PROFILE]

# Set a permanent password
aws cognito-idp admin-set-user-password \
  --user-pool-id $USER_POOL_ID \
  --username [EMAIL] \
  --password '[PASSWORD]' \
  --permanent \
  --profile [YOUR_AWS_PROFILE]
```

## Part 3: Copy Configuration Files

Copy `amplify_outputs.json` to the test resources directory so instrumentation tests can find it:

```bash
cp amplify_outputs.json \
  aws-kinesis/src/androidTest/res/raw/amplify_outputs.json
```

Create the credentials file:

```bash
cat > /path/to/amplify-android/aws-kinesis/src/androidTest/res/raw/credentials.json << 'EOF'
{
  "username": "[EMAIL]",
  "password": "[PASSWORD]"
}
EOF
```
