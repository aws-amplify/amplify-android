/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthRuleProvider;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache of auth provider chains by model/operation.
 */
public final class AuthProviderChainRepository {
    private static final Logger LOG = Amplify.Logging.forNamespace("ampify:aws-api");
    //TODO: Use enums for keys. Right now we have a couple different enums, so we may need to consolidate.
    private Map<String, Map<String, AuthProviderChain>> authProviderChains;

    /**
     * Convenience constructor if the caller want to pass in a map.
     * @param modelSchemas List of schemas for the graphQL APIs
     */
    public AuthProviderChainRepository(Map<String, ModelSchema> modelSchemas) {
        this(new ArrayList<ModelSchema>(modelSchemas.values()));
    }

    /**
     * Default constructor.
     * TODO: better comments.
     * @param modelSchemas The list of model schemas to use when building the auth chain.
     */
    public AuthProviderChainRepository(List<ModelSchema> modelSchemas) {
        authProviderChains = new HashMap<>();
        for (ModelSchema modelSchema : modelSchemas) {
            Map<String, AuthProviderChain> modelAuthProviderChain = new HashMap<>();
            authProviderChains.put(modelSchema.getName(), modelAuthProviderChain);
            for (ModelOperation operation : ModelOperation.values()) {
                List<AuthRule> applicableRules = modelSchema.getApplicableRules(operation);
                AuthProviderChain modelOpChain = new AuthProviderChain(modelSchema.getName(), operation.name());
                for (AuthRule rule : applicableRules) {
                    for (AuthRuleProvider provider : rule.getAuthStrategy().getCompatibleProviders()) {
                        modelOpChain.addProvider(provider);
                    }
                }
                modelAuthProviderChain.put(operation.name(), modelOpChain);
            }
        }
    }

    /**
     * Get the provider chain by model and operation type.
     * @param modelName The name of the model (name field from ModelSchema type).
     * @param operation The operation type (READ, CREATE, UPDATE, DELETE).
     * @return The auth provider chain for the given parameters.
     */
    public AuthProviderChain getChain(String modelName, String operation) {
        //TODO: Check for missing item
        return authProviderChains != null ? authProviderChains.get(modelName).get(operation) : null;
    }

    /**
     * Resets the active auth mode for all model/operation combinations currently in the cache.
     */
    public void reset() {
        for (Map<String, AuthProviderChain> modelChain : authProviderChains.values()) {
            modelChain.values().forEach(AuthProviderChain::reset);
        }
    }

    static class AuthProviderChain {
        private final String modelName;
        private final String operation;
        private final List<AuthRuleProvider> providers;
        private AuthProviderChainNode head;
        private AuthProviderChainNode tail;
        private AuthProviderChainNode current;

        AuthProviderChain(String modelname,
                          String operation) {
            this.providers = new ArrayList<>();
            this.modelName = modelname;
            this.operation = operation;
        }

        public void addProvider(AuthRuleProvider provider) {
            if (!providers.contains(provider)) {
                LOG.debug("Before adding " + provider.name() + " to " + this.toString());
                if (head == null) { // chain is empty
                    head = new AuthProviderChainNode(provider, null);
                    tail = head;
                } else {
                    tail.next = new AuthProviderChainNode(provider, null);
                    tail = tail.next;
                }
                providers.add(provider);
                LOG.debug("After adding " + provider.name() + " to " + this.toString());
            }
        }

        public AuthRuleProvider getCurrent() {
            if (current == null) {
                current = head;
            }
            return current.provider;
        }

        public boolean nextProvider() {
            LOG.debug("Before moving next " + this.toString());
            if (current == null) { // Set current to be the first provider
                if (head != null) {
                    current = head;
                    return true;
                } else {
                    return false;
                }
            } else if (current.next != null) { // Advance
                this.current = current.next;
                return true;
            } else {
                this.current = null;
                return false;
            }
        }

        public void reset() {
            this.current = null;
        }

        @NonNull
        @Override
        public String toString() {
            return modelName + "(" + operation + ")" +
                    " Head = " + head +
                    " Tail = " + tail +
                    " Current = " + current;
        }
    }

    static class AuthProviderChainNode {
        private AuthRuleProvider provider;
        private AuthProviderChainNode next;

        /**
         * Construct a node for the given auth provider.
         * @param provider The auth provider for the node.
         * @param next The next provider in the chain (if any).
         */
        AuthProviderChainNode(AuthRuleProvider provider,
                              AuthProviderChainNode next) {
            this.provider = provider;
            this.next = next;
        }

        @NonNull
        @Override
        public String toString() {
            return provider.name() + " Has Next = " + (next != null);
        }
    }
}
