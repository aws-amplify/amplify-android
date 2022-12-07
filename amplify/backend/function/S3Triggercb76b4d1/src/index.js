/*
Use the following code to retrieve configured secrets from SSM:

const aws = require('aws-sdk');

const { Parameters } = await (new aws.SSM())
  .getParameters({
    Names: ["space_key","space_secret"].map(secretName => process.env[secretName]),
    WithDecryption: true,
  })
  .promise();

Parameters will be of the form { Name: 'secretName', Value: 'secretValue', ... }[]
*/

var aws = require('aws-sdk');

const env = process.env.ENV;
const spaceBucket = process.env.SPACE_BUCKET;
const spaceRegion = process.env.SPACE_REGION;
const spaceEndpoint = process.env.SPACE_ENDPOINT;
const objectPrex = process.env.OBJECT_PREFIXES;

const eventPrefix = "ObjectCreated";
const prefixDelimiter = ";;";
const prodEnvName = "prod";

exports.handler = async function (event) {
    console.log('Received S3 event:', JSON.stringify(event, null, 2));
    
    const eventName = event.Records[0].eventName;
    const bucket = event.Records[0].s3.bucket.name;
    const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, ' '));
    console.log(`Env: ${env}, EventName: ${eventName}, Bucket: ${bucket}`, `Key: ${key}`);
    if (!eventName.startsWith(eventPrefix)) {
        console.log(`event ${eventName} no need to sync.`)
        return
    }

    let objectPrefixes = objectPrex.split(prefixDelimiter);
    // console.log(`env object prefixes: ${objectPrex}, split object prefixes ${objectPrefixes}`);
    let isPrefixMatch = false;
    for (let i = 0; i < objectPrefixes.length; i++) {
        console.log(`prefix ${i} ${objectPrefixes[i]}`)
        if (key.startsWith(objectPrefixes[i])) {
            isPrefixMatch = true;
            break;
        }
    }
    console.log(`prefix match: ${isPrefixMatch}`)
    if (!isPrefixMatch) {
        console.log(`path ${key} no need to sync.`)
        return
    }
    const { Parameters } = await (new aws.SSM())
        .getParameters({
            Names: ["space_key", "space_secret"].map(secretName => process.env[secretName]),
            WithDecryption: true,
        })
        .promise();
    // console.log("parameters:", Parameters);
    var spaceKey = "";
    var spaceSecret = "";
    for (let i = 0; i < Parameters.length; i++) {
        if (Parameters[i].Name.endsWith("space_key")) {
            spaceKey = Parameters[i].Value;
        } else if (Parameters[i].Name.endsWith("space_secret")) {
            spaceSecret = Parameters[i].Value;
        }
    }
    console.log(`space bucket: ${spaceBucket}, region: ${spaceRegion}, endpoint: ${spaceEndpoint}`);

    var s3 = new aws.S3();
    var spaceConfig = {
        bucket: spaceBucket,
        endpoint: spaceEndpoint,
        region: spaceRegion,
        credentials: {
            accessKeyId: spaceKey,
            secretAccessKey: spaceSecret
        }
    };
    var spaceClient = new aws.S3(spaceConfig);

    var obj = await s3.getObject({ Bucket: bucket, Key: key }).promise();
    console.log("obj:", obj);
    let bucketParams = {
        Bucket: spaceBucket,
        Key: key,
        ACL: 'public-read',
        Body: obj.Body,
        ContentType: obj.ContentType,
    };

    if (env === prodEnvName) {
        try {
            const putResp = await spaceClient.putObject(bucketParams).promise();
            console.log("Successfully uploaded object:", putResp);
        } catch (err) {
            console.log("Error", err);
        }
    } else {
        console.log(`env ${env}, do not sync, only log.`)
    }

};