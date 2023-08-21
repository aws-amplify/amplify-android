/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.options;

import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions;

/**
 * Cognito extension of update verify totp setup options to add the platform specific fields.
 */
public final class AWSCognitoAuthVerifyTOTPSetupOptions extends AuthVerifyTOTPSetupOptions {

    private String friendlyDeviceName;

    private AWSCognitoAuthVerifyTOTPSetupOptions(String friendlyDeviceName) {
        this.friendlyDeviceName = friendlyDeviceName;
    }

    /**
     * Return the friendlyDeviceName to set during cognito TOTP setup.
     * @return friendlyDeviceName string
     * */
    public String getFriendlyDeviceName() {
        return friendlyDeviceName;
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private String friendlyDeviceName;

        private String getFriendlyDeviceName() {
            return friendlyDeviceName;
        }

        /**
         * Friendly device name to be set in Cognito.
         * @param friendlyDeviceName String input for friendlyDeviceName
         * @return current CognitoBuilder instance
         * */
        public CognitoBuilder setFriendlyDeviceName(String friendlyDeviceName) {
            this.friendlyDeviceName = friendlyDeviceName;
            return this;
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthVerifyTOTPSetupOptions with the values specified in the builder.
         */
        public AWSCognitoAuthVerifyTOTPSetupOptions build() {
            return new AWSCognitoAuthVerifyTOTPSetupOptions(getFriendlyDeviceName());
        }
    }
}
