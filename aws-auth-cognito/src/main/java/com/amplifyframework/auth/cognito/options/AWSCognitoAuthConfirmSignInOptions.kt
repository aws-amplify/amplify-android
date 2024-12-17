/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.auth.cognito.options

import android.app.Activity
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import java.lang.ref.WeakReference

/**
 * Cognito extension of confirm sign in options to add the platform specific fields.
 */
data class AWSCognitoAuthConfirmSignInOptions internal constructor(
    /**
     * Get custom attributes to be sent to the service such as information about the client.
     * @return custom attributes to be sent to the service such as information about the client
     */
    val metadata: Map<String, String>,
    /**
     * Get additional user attributes which should be associated with this user on confirmSignIn.
     * @return additional user attributes which should be associated with this user on confirmSignIn
     */
    val userAttributes: List<AuthUserAttribute>,
    /**
     * Get the friendly device name used to setup TOTP.
     * @return friendly device name
     */
    val friendlyDeviceName: String?,
    /**
     * Get the Activity instance, if any.
     * @return A WeakReference to the Activity
     */
    val callingActivity: WeakReference<Activity>
) : AuthConfirmSignInOptions() {

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        @JvmStatic
        fun builder(): CognitoBuilder = CognitoBuilder()

        inline operator fun invoke(block: CognitoBuilder.() -> Unit) = CognitoBuilder().apply(block).build()
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder?>() {
        private var metadata: Map<String, String> = mapOf()
        private var userAttributes: List<AuthUserAttribute> = listOf()
        private var friendlyDeviceName: String? = null
        private var callingActivity: WeakReference<Activity> = WeakReference(null)

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder = this

        /**
         * Set the metadata field for the object being built.
         * @param metadata Custom user metadata to be sent with the sign in request.
         * @return The builder object to continue building.
         */
        fun metadata(metadata: Map<String, String>) = apply { this.metadata = metadata }

        /**
         * Set the userAttributes field for the object being built.
         * @param userAttributes A list of additional user attributes which should be
         * * associated with this user on confirmSignIn.
         * @return the instance of the builder.
         */
        fun userAttributes(userAttributes: List<AuthUserAttribute>) = apply { this.userAttributes = userAttributes }

        /**
         * Set the friendlyDeviceName field for the object being built.
         * @param friendlyDeviceName friendly name of the device used to setup totp.
         * @return the instance of the builder.
         */
        fun friendlyDeviceName(friendlyDeviceName: String) = apply { this.friendlyDeviceName = friendlyDeviceName }

        /**
         * Set the callingActivity field for the object being built. This should be set when using WebAuthn to ensure
         * the optimal user experience.
         * @param callingActivity The current Activity.
         * @return the instance of the builder.
         */
        fun callingActivity(callingActivity: Activity) = apply {
            this.callingActivity = WeakReference(callingActivity)
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthConfirmSignInOptions with the values specified in the builder.
         */
        override fun build() =
            AWSCognitoAuthConfirmSignInOptions(metadata, userAttributes, friendlyDeviceName, callingActivity)
    }
}
