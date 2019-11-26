/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model.types;

/**
 * An enumeration of the various AWS AppSync scalar types.
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">Appsync Scalars</a>
 */
@SuppressWarnings("checkstyle:LineLength") // Hyperlinks are long! Fact of life.
public enum AWSAppSyncScalarType {

    /**
     * A unique identifier.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#id">ID Scalar</a>
     */
    ID("ID"),

    /**
     * Textual data.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#string">String Scalar</a>
     */
    STRING("String"),

    /**
     * Non-fractional signed whole numeric values in range -(2^31) to 2^31 - 1.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#int">Int Scalar</a>
     */
    INT("Int"),

    /**
     * A signed double-precision fractional values as specified by IEEE 754.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#float">Float Scalar</a>
     */
    FLOAT("Float"),

    /**
     * A boolean value of either true or false.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#boolean">Boolean Scalar</a>
     */
    BOOLEAN("Boolean"),

    /**
     * An extended ISO 8601 Date string.
     * For example, 1970-01-01Z, 1970-01-01-07:00 and 1970-01-01+05:30 are all valid dates.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsdate">AWSDate Scalar</a>
     */
    AWS_DATE("AWSDate"),

    /**
     * A valid extended ISO 8601 Time string.
     * For example, 12:30Z, 12:30:24-07:00 and 12:30:24.500+05:30 are all valid time strings.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awstime">AWSTime Scalar</a>
     */
    AWS_TIME("AWSTime"),

    /**
     * A valid extended ISO 8601 DateTime string.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsdatetime">AWSDateTime Scalar</a>
     */
    AWS_DATE_TIME("AWSDateTime"),

    /**
     * A number of seconds that have elapsed since 1970-01-01T00:00Z.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awstimestamp">AWSTimestamp Scalar</a>
     */
    AWS_TIMESTAMP("AWSTimestamp"),

    /**
     * An Email address string that complies with RFC 822.
     * For example, username@example.com is a valid Email address.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsemail">AWSEmail Scalar</a>
     */
    AWS_EMAIL("AWSEmail"),

    /**
     * A JSON string that complies with RFC 8259.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsjson">AWSJSON Scalar</a>
     */
    AWS_JSON("AWSJSON"),

    /**
     * A valid URL string.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsurl">AWSURL Scalar</a>
     */
    AWS_URL("AWSURL"),

    /**
     * A valid Phone Number.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsphone">AWSPhone Scalar</a>
     */
    AWS_PHONE("AWSPhone"),

    /**
     * A valid IPv4 or IPv6 address string.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awsipaddress">AWSIPAddress Scalar</a>
     */
    AWS_IP_ADDRESS("AWSIPAddress");

    private final String stringValue;

    AWSAppSyncScalarType(final String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Gets the value of the data type, expressed as a string.
     * @return Value of data type expressed as a string
     */
    public String stringValue() {
        return stringValue;
    }

    @Override
    public String toString() {
        return stringValue();
    }

    /**
     * Apply the enumeration constraint to an arbitrary string, to determined if it is one
     * of the enumerated values.
     * @param string An arbitrary string, possibly a string representation of one of the
     *               enumerated {@link AWSAppSyncScalarType}.
     * @return An {@link AWSAppSyncScalarType}, if provided string is one of the enumerated values
     * @throws IllegalArgumentException If provided stirng is not one of the enumerated values
     */
    public static AWSAppSyncScalarType fromString(final String string) {
        for (AWSAppSyncScalarType possibleMatch : values()) {
            if (possibleMatch.stringValue.equalsIgnoreCase(string)) {
                return possibleMatch;
            }
        }
        throw new IllegalArgumentException("Provided value of " + string + " is not a AWSAppSyncScalarType.");
    }
}
