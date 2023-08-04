/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

/**
 * Factory for preset user attributes and the ability to specify a custom one.
 */
public final class AuthUserAttributeKey {
    private static final String ADDRESS = "address";
    private static final String BIRTHDATE = "birthdate";
    private static final String EMAIL = "email";
    private static final String EMAIL_VERIFIED = "email_verified";
    private static final String FAMILY_NAME = "family_name";
    private static final String GENDER = "gender";
    private static final String GIVEN_NAME = "given_name";
    private static final String LOCALE = "locale";
    private static final String MIDDLE_NAME = "middle_name";
    private static final String NICKNAME = "nickname";
    private static final String PHONE_NUMBER = "phone_number";
    private static final String PHONE_NUMBER_VERIFIED = "phone_number_verified";
    private static final String PICTURE = "picture";
    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String PROFILE = "profile";
    private static final String UPDATED_AT = "updated_at";
    private static final String WEBSITE = "website";
    private static final String ZONE_INFO = "zoneinfo";
    private static final String NAME = "name";

    private final String attributeKey;

    /**
     * Construct a new instance of AuthUserAttributeKey.
     * @param attributeKey the attribute key to use in the new object.
     */
    private AuthUserAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    /**
     * Factory method for getting address attribute.
     * @return a pre-configured address attribute
     */
    public static AuthUserAttributeKey address() {
        return new AuthUserAttributeKey(ADDRESS);
    }

    /**
     * Factory method for getting birthdate attribute.
     * @return a pre-configured birthdate attribute
     */
    public static AuthUserAttributeKey birthdate() {
        return new AuthUserAttributeKey(BIRTHDATE);
    }

    /**
     * Factory method for getting email attribute.
     * @return a pre-configured email attribute
     */
    public static AuthUserAttributeKey email() {
        return new AuthUserAttributeKey(EMAIL);
    }

    /**
     * Factory method for getting email verified attribute.
     * @return a pre-configured email verified attribute
     */
    public static AuthUserAttributeKey emailVerified() {
        return new AuthUserAttributeKey(EMAIL_VERIFIED);
    }

    /**
     * Factory method for getting family name attribute.
     * @return a pre-configured family name attribute
     */
    public static AuthUserAttributeKey familyName() {
        return new AuthUserAttributeKey(FAMILY_NAME);
    }

    /**
     * Factory method for getting gender attribute.
     * @return a pre-configured gender attribute
     */
    public static AuthUserAttributeKey gender() {
        return new AuthUserAttributeKey(GENDER);
    }

    /**
     * Factory method for getting given name attribute.
     * @return a pre-configured given name attribute
     */
    public static AuthUserAttributeKey givenName() {
        return new AuthUserAttributeKey(GIVEN_NAME);
    }

    /**
     * Factory method for getting locale attribute.
     * @return a pre-configured locale attribute
     */
    public static AuthUserAttributeKey locale() {
        return new AuthUserAttributeKey(LOCALE);
    }

    /**
     * Factory method for getting middle name attribute.
     * @return a pre-configured middle name attribute
     */
    public static AuthUserAttributeKey middleName() {
        return new AuthUserAttributeKey(MIDDLE_NAME);
    }

    /**
     * Factory method for getting name attribute.
     * @return a pre-configured name attribute
     */
    public static AuthUserAttributeKey name() {
        return new AuthUserAttributeKey(NAME);
    }

    /**
     * Factory method for getting nickname attribute.
     * @return a pre-configured nickname attribute
     */
    public static AuthUserAttributeKey nickname() {
        return new AuthUserAttributeKey(NICKNAME);
    }

    /**
     * Factory method for getting phone number attribute.
     * @return a pre-configured phone number attribute
     */
    public static AuthUserAttributeKey phoneNumber() {
        return new AuthUserAttributeKey(PHONE_NUMBER);
    }

    /**
     * Factory method for getting phone number verified attribute.
     * @return a pre-configured phone number attribute
     */
    public static AuthUserAttributeKey phoneNumberVerified() {
        return new AuthUserAttributeKey(PHONE_NUMBER_VERIFIED);
    }

    /**
     * Factory method for getting picture attribute.
     * @return a pre-configured picture attribute
     */
    public static AuthUserAttributeKey picture() {
        return new AuthUserAttributeKey(PICTURE);
    }

    /**
     * Factory method for getting preferred username attribute.
     * @return a pre-configured preferred username attribute
     */
    public static AuthUserAttributeKey preferredUsername() {
        return new AuthUserAttributeKey(PREFERRED_USERNAME);
    }

    /**
     * Factory method for getting profile attribute.
     * @return a pre-configured profile attribute
     */
    public static AuthUserAttributeKey profile() {
        return new AuthUserAttributeKey(PROFILE);
    }

    /**
     * Factory method for getting updated at attribute.
     * @return a pre-configured updated at attribute
     */
    public static AuthUserAttributeKey updatedAt() {
        return new AuthUserAttributeKey(UPDATED_AT);
    }

    /**
     * Factory method for getting website attribute.
     * @return a pre-configured website attribute
     */
    public static AuthUserAttributeKey website() {
        return new AuthUserAttributeKey(WEBSITE);
    }

    /**
     * Factory method for getting zone info attribute.
     * @return a pre-configured zone info attribute
     */
    public static AuthUserAttributeKey zoneInfo() {
        return new AuthUserAttributeKey(ZONE_INFO);
    }

    /**
     * Factory method for creating your own custom user attribute.
     * @param attributeKey The key for the custom user attribute
     * @return a custom provider
     */
    public static AuthUserAttributeKey custom(String attributeKey) {
        return new AuthUserAttributeKey(attributeKey);
    }

    /**
     * Returns the String key for the attribute.
     * @return the String key for the attribute
     */
    @NonNull
    public String getKeyString() {
        return attributeKey;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getKeyString()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthUserAttributeKey attributeKey = (AuthUserAttributeKey) obj;
            return ObjectsCompat.equals(getKeyString(), attributeKey.getKeyString());
        }
    }

    @Override
    public String toString() {
        return "AuthUserAttributeKey {" +
                "attributeKey=" + attributeKey +
                '}';
    }
}
