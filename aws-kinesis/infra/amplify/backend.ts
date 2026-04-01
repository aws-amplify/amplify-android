import { defineBackend } from '@aws-amplify/backend';
import { auth } from './auth/resource';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
import * as firehose from 'aws-cdk-lib/aws-kinesisfirehose';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Duration, RemovalPolicy } from 'aws-cdk-lib';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';

const backend = defineBackend({
  auth,
});

// ---------------------------------------------------------------
// Kinesis Data Stream
// ---------------------------------------------------------------

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

// ---------------------------------------------------------------
// Firehose Delivery Stream
// ---------------------------------------------------------------

const firehoseStack = backend.createStack('FirehoseStack');

// S3 bucket as the Firehose destination (required)
const firehoseBucket = new s3.Bucket(firehoseStack, 'FirehoseDestinationBucket', {
  removalPolicy: RemovalPolicy.DESTROY,
  autoDeleteObjects: true,
  lifecycleRules: [
    { expiration: Duration.days(1) }, // auto-clean test data
  ],
});

// IAM role for Firehose to write to S3
const firehoseRole = new Role(firehoseStack, 'FirehoseDeliveryRole', {
  assumedBy: new ServicePrincipal('firehose.amazonaws.com'),
});
firehoseBucket.grantReadWrite(firehoseRole);

const deliveryStream = new firehose.CfnDeliveryStream(firehoseStack, 'TestDeliveryStream', {
  deliveryStreamName: 'amplify-firehose-test-stream',
  s3DestinationConfiguration: {
    bucketArn: firehoseBucket.bucketArn,
    roleArn: firehoseRole.roleArn,
    bufferingHints: {
      intervalInSeconds: 60,
      sizeInMBs: 1,
    },
  },
});

// Grant authenticated users permission to put records to Firehose
backend.auth.resources.authenticatedUserIamRole.addToPrincipalPolicy(
  new PolicyStatement({
    actions: [
      'firehose:PutRecord',
      'firehose:PutRecordBatch',
      'firehose:DescribeDeliveryStream',
    ],
    resources: [deliveryStream.attrArn],
  })
);
