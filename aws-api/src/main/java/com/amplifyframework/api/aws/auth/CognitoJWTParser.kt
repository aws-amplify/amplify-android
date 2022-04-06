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

package com.amplifyframework.api.aws.auth

import android.util.Base64
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import org.json.JSONException
import org.json.JSONObject

/**
 * Utility class for all operations on JWT.\
 */
class CognitoJWTParser {

    companion object {
        /**
         * Returns payload of a JWT as a JSON object.
         *
         * @param jwt       REQUIRED: valid JSON Web Token as String.
         * @return payload as a JSONObject.
         */
        fun getPayload(jwt: String): JSONObject {
            return try {
                validateJWT(jwt)
                val payload = jwt.split(".")[PAYLOAD]
                val sectionDecoded = Base64.decode(payload, Base64.URL_SAFE)
                val jwtSection = String(sectionDecoded, Charset.forName("UTF-8"))
                JSONObject(jwtSection)
            } catch (e: UnsupportedEncodingException) {
                throw CognitoParameterInvalidException(e.message)
            } catch (e: JSONException) {
                throw CognitoParameterInvalidException(e.message)
            } catch (e: Exception) {
                throw CognitoParameterInvalidException("error in parsing JSON")
            }
        }

        /**
         * Checks if `JWT` is a valid JSON Web Token.
         *
         * @param jwt REQUIRED: The JWT as a [String].
         */
        private fun validateJWT(jwt: String) {
            // Check if the the JWT has the three parts
            val jwtParts = jwt.split(".")
            if (jwtParts.size != JWT_PARTS) {
                throw CognitoParameterInvalidException("not a JSON Web Token")
            }
        }

        private val PAYLOAD = 1
        private val JWT_PARTS = 3
    }
}

class CognitoParameterInvalidException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)
