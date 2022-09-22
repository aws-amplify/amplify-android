package com.amplifyframework.auth.cognito

import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.sdk.kotlin.runtime.endpoint.CredentialScope
import aws.smithy.kotlin.runtime.http.endpoints.Endpoint

internal class AWSEndpointResolver(val endpoint: Endpoint) : AwsEndpointResolver {
    override suspend fun resolve(service: String, region: String): AwsEndpoint {
        return AwsEndpoint(endpoint.uri, CredentialScope(region, service))
    }
}
