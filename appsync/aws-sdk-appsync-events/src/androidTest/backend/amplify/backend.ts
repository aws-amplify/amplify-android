import { defineBackend } from '@aws-amplify/backend'
import { auth } from './auth/resource'
import {
	AuthorizationType,
	CfnApi,
	CfnChannelNamespace,
	CfnApiKey,
} from 'aws-cdk-lib/aws-appsync'
import { Policy, PolicyStatement } from 'aws-cdk-lib/aws-iam'

const backend = defineBackend({ auth })

const customResources = backend.createStack('custom-resources-appsync-events')

const cfnEventAPI = new CfnApi(customResources, 'cfnEventAPI', {
	name: 'appsync-events-integration-tests',
	eventConfig: {
		authProviders: [
			{ authType: AuthorizationType.API_KEY },
			{ authType: AuthorizationType.IAM },
			{
				authType: AuthorizationType.USER_POOL,
				cognitoConfig: {
				  awsRegion: customResources.region,
				  userPoolId: backend.auth.resources.userPool.userPoolId,
				},
			}
		],
		connectionAuthModes: [
			{ authType: AuthorizationType.API_KEY },
			{ authType: AuthorizationType.IAM },
			{ authType: AuthorizationType.USER_POOL }
		],
		defaultPublishAuthModes: [
			{ authType: AuthorizationType.API_KEY },
			{ authType: AuthorizationType.IAM },
			{ authType: AuthorizationType.USER_POOL }
		],
		defaultSubscribeAuthModes: [
			{ authType: AuthorizationType.API_KEY },
			{ authType: AuthorizationType.IAM },
			{ authType: AuthorizationType.USER_POOL }
		],
	},
})

new CfnChannelNamespace(customResources, 'CfnEventsIntegrationTestsNamespace', {
	apiId: cfnEventAPI.attrApiId,
	name: 'default',
})

new CfnChannelNamespace(customResources, 'CfnEventsIntegrationTestsCustomNamespace', {
	apiId: cfnEventAPI.attrApiId,
	name: 'custom',
})

// attach a policy to the authenticated user role in our User Pool to grant access to the Event API:
backend.auth.resources.authenticatedUserIamRole.attachInlinePolicy(
	new Policy(customResources, 'AuthAppSyncEventPolicy', {
	  statements: [
		new PolicyStatement({
		  actions: [
			'appsync:EventConnect',
			'appsync:EventSubscribe',
			'appsync:EventPublish',
		  ],
		  resources: [`${cfnEventAPI.attrApiArn}/*`, `${cfnEventAPI.attrApiArn}`],
		}),
	  ],
	})
);

// Add the policy as an inline policy (not `addToPrincialPolicy`) to avoid circular deps
backend.auth.resources.unauthenticatedUserIamRole.attachInlinePolicy(
	new Policy(customResources, 'UnauthAppSyncEventPolicy', {
		statements: [
			new PolicyStatement({
				actions: [
					'appsync:EventConnect',
					'appsync:EventPublish',
					'appsync:EventSubscribe',
				],
				resources: [`${cfnEventAPI.attrApiArn}/*`, `${cfnEventAPI.attrApiArn}`],
			}),
		],
	})
)

// Create an API key
const apiKey = new CfnApiKey(customResources, 'EventApiKey', {
    apiId: cfnEventAPI.attrApiId,
    description: 'API Key for Event API',
    expires: Math.floor(Date.now() / 1000) + (37 * 24 * 60 * 60) // Set for 37 days
})

backend.addOutput({
	custom: {
		events: {
			url: `https://${cfnEventAPI.getAtt('Dns.Http').toString()}/event`,
			aws_region: customResources.region,
			default_authorization_type: AuthorizationType.API_KEY,
			api_key: apiKey.attrApiKey
		},
	},
})
