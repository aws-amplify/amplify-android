package com.amplifyframework.auth.cognito.exceptions.configuration

import com.amplifyframework.auth.exceptions.ConfigurationException

/**
 * Could not perform the action because Oauth is not configured or
 * is configured incorrectly.
 */
class InvalidOauthConfigurationException : ConfigurationException(
    message = "The Oauth configuration is missing or invalid.",
    recoverySuggestion = "HostedUI Oauth section not configured or unable to parse from amplifyconfiguration.json file."
)
