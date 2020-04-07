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

package com.amplifyframework.storage.s3;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.testutils.Resources;
import com.amplifyframework.util.Immutable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class UserCredentials implements Iterable<UserCredentials.Credential> {
    private static final String CREDENTIALS_RESOURCE_NAME = "credentials";
    private final List<Credential> credentials;

    private UserCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }

    static UserCredentials create(@NonNull IdentityIdSource identityIdSource, @NonNull Context context) {
        Objects.requireNonNull(identityIdSource);
        Objects.requireNonNull(context);

        @RawRes int resourceId = Resources.getRawResourceId(context, CREDENTIALS_RESOURCE_NAME);
        Map<String, String> userAndPasswordMap = readCredentialsFromResource(context, resourceId);

        final List<Credential> credentials = new ArrayList<>();
        for (Map.Entry<String, String> entry : userAndPasswordMap.entrySet()) {
            String username = entry.getKey();
            String password = entry.getValue();
            String identityId = identityIdSource.fetchIdentityId(username, password);
            credentials.add(Credential.create(identityId, username, password));
        }
        return new UserCredentials(Immutable.of(credentials));
    }

    private static Map<String, String> readCredentialsFromResource(Context context, @RawRes int resourceId) {
        JSONObject resource = Resources.readAsJson(context, resourceId);
        Map<String, String> userCredentials = new HashMap<>();
        try {
            JSONArray credentials = resource.getJSONArray("credentials");
            for (int index = 0; index < credentials.length(); index++) {
                JSONObject credential = credentials.getJSONObject(index);
                String username = credential.getString("username");
                String password = credential.getString("password");
                userCredentials.put(username, password);
            }
            return userCredentials;
        } catch (JSONException jsonReadingFailure) {
            throw new RuntimeException(jsonReadingFailure);
        }
    }

    @NonNull
    @Override
    public Iterator<Credential> iterator() {
        return credentials.iterator();
    }

    static final class Credential {
        private final String identityId;
        private final String username;
        private final String password;

        private Credential(String identityId, String username, String password) {
            this.identityId = identityId;
            this.username = username;
            this.password = password;
        }

        @NonNull
        static Credential create(
                @NonNull String identityId, @NonNull String username, @NonNull String password) {
            Objects.requireNonNull(identityId);
            Objects.requireNonNull(username);
            Objects.requireNonNull(password);
            return new Credential(identityId, username, password);
        }

        @NonNull
        String getIdentityId() {
            return identityId;
        }

        @NonNull
        String getUsername() {
            return username;
        }

        @NonNull
        String getPassword() {
            return password;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Credential that = (Credential) thatObject;

            if (!ObjectsCompat.equals(identityId, that.identityId)) {
                return false;
            }
            if (!ObjectsCompat.equals(username, that.username)) {
                return false;
            }
            return ObjectsCompat.equals(password, that.password);
        }

        @Override
        public int hashCode() {
            int result = identityId.hashCode();
            result = 31 * result + username.hashCode();
            result = 31 * result + password.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Credential{" +
                "identityId='" + identityId + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
        }
    }

    /**
     * A component which is able to produce an identity id, from a username/password.
     */
    interface IdentityIdSource {
        /**
         * Lookup an Identity ID from a username and password.
         * @param username A username
         * @param password A password
         * @return An identity ID associated with username and password
         * @throws IllegalArgumentException If there is no identity ID for the requested username and password.
         */
        @NonNull
        String fetchIdentityId(@NonNull String username, @NonNull String password) throws IllegalArgumentException;
    }
}
