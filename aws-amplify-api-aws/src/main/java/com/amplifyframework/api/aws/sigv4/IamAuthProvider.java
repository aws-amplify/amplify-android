package com.amplifyframework.api.aws.sigv4;

import com.amazonaws.auth.AWSCredentialsProvider;

/**
 * Interface for IAM Auth provider
 */
public interface IamAuthProvider extends AuthProvider, AWSCredentialsProvider {
}
