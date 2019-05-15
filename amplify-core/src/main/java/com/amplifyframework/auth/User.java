/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amplifyframework.auth;

import java.util.Map;

public class User {
    private String userId;
    private Map<String, String> userAttributes;
    private UserState userState;

    public String getUserId() {
        return userId;
    }

    synchronized void updateUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, String> getUserAttributes() {
        return userAttributes;
    }

    public synchronized void updateUserAttributes(Map<String, String> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public synchronized void updateUserAttribute(String attributeName, String attributeValue) {
        this.userAttributes.put(attributeName, attributeValue);
    }

    public UserState getUserState() {
        return userState;
    }

    synchronized void updateUserState(UserState userState) {
        this.userState = userState;
    }
}
