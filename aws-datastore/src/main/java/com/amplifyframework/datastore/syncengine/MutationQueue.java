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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link MutationQueue} is a LinkedHashMap like container , the goal of using this container is to
 * achieve O(1) time complexity for both getting a {@link PendingMutation} and update an existing mutation with
 * valid id.
 * MutationQueue is implementing the Queue interface and provide most of the queue operations.
 * 
 * @deprecated This class was released with public visibility but is not intended to be consumed.
 *             It will be removed in future versions of Amplify.
 */
@Deprecated
public final class MutationQueue {

    private final Map<TimeBasedUuid, Node> mutationMap = new HashMap<>();
    private final Node dummyHead;
    private final Node dummyTail;

    /**
     * Default constructor for {@link MutationQueue}.
     */
    public MutationQueue() {
        dummyHead = new Node();
        dummyTail = new Node();
        dummyHead.next = dummyTail;
        dummyTail.prev = dummyHead;
    }

    /**
     * Find the first Pending Mutation which its model has the same id.
     *
     * @param modelId the model id
     * @return the {@link PendingMutation} instance
     */
    synchronized PendingMutation<? extends Model> nextMutationForModelId(String modelId) {
        Node head = dummyHead.next;
        if (head == dummyTail) {
            return null;
        }
        while (head != dummyTail) {
            if (head.mutation.getMutatedItem().getPrimaryKeyString().equals(modelId)) {
                return head.mutation;
            }
            head = head.next;
        }
        return null;
    }

    /**
     * Remove the {@link PendingMutation} from {@link MutationQueue} by its Id.
     * this operation should be consuming constant time.
     *
     * @param timeBasedUuid the {@link TimeBasedUuid} UUID of a PendingMutation in @{@link #mutationMap}
     * @return {@link Boolean} return true if remove is successful
     */
    synchronized boolean removeById(TimeBasedUuid timeBasedUuid) {
        if (!mutationMap.containsKey(timeBasedUuid)) {
            return false;
        }
        Node removingNode = mutationMap.get(timeBasedUuid);
        Node removingPrev = removingNode.prev;
        Node removingNext = removingNode.next;
        //remove from map
        mutationMap.remove(timeBasedUuid);
        //remove from current linkedlist
        removingPrev.next = removingNext;
        removingNext.prev = removingPrev;
        return true;
    }

    /**
     * Add a {@link PendingMutation} to the tail of this Queue.
     *
     * @param pendingMutation {@link PendingMutation}
     * @return true if successfully added a pending mutation
     */
    private synchronized boolean addToTail(@NonNull PendingMutation<? extends Model> pendingMutation) {
        //constructing a new node
        Node pendingNode = new Node();
        pendingNode.id = pendingMutation.getMutationId();
        pendingNode.mutation = pendingMutation;
        //insert the new node into the current linkedlist
        Node addingPrev = dummyTail.prev;
        addingPrev.next = pendingNode;
        dummyTail.prev = pendingNode;
        pendingNode.prev = addingPrev;
        pendingNode.next = dummyTail;
        //put the mutation into the mutationmap
        mutationMap.put(pendingMutation.getMutationId(), pendingNode);
        return true;
    }

    /**
     * Replace an existing {@link PendingMutation} inside the Queue.
     *
     * @param timeBasedUuid              the UUID of a pending mutation
     * @param pendingMutation the pending mutation's instance
     */
    synchronized void updateExistingQueueItemOrAppendNew(@NonNull TimeBasedUuid timeBasedUuid,
                                                         @NonNull PendingMutation<? extends Model> pendingMutation) {
        // If there is already a mutation with same ID in the queue,
        // we'll go find it, and then update it, with this contents.
        if (mutationMap.containsKey(timeBasedUuid)) {
            mutationMap.get(timeBasedUuid).mutation = pendingMutation;
        } else {
            // Otherwise, just add it to the end of the queue.
            addToTail(pendingMutation);
        }
    }

    /**
     * Get a {@link PendingMutation} instance by its UUID from {@link #mutationMap}.
     *
     * @param timeBasedUuid the UUID of a pending mutation
     * @return an instance of {@link PendingMutation}
     */
    @Nullable // When there is no match.
    synchronized PendingMutation<? extends Model> getMutationById(TimeBasedUuid timeBasedUuid) {
        if (mutationMap.containsKey(timeBasedUuid)) {
            return mutationMap.get(timeBasedUuid).mutation;
        }
        return null;
    }

    /**
     * Determine if the queue is empty.
     *
     * @return true if queue is empty
     */
    public synchronized boolean isEmpty() {
        return mutationMap.isEmpty();
    }

    /**
     * Add a {@link PendingMutation} instance to the tail of the queue.
     *
     * @param pendingMutation the mutation instance
     * @return return true if we successfully added the pending mutation into the queue
     */
    public boolean add(PendingMutation<? extends Model> pendingMutation) {
        if (pendingMutation != null && !mutationMap.containsKey(pendingMutation.getMutationId())) {
            return addToTail(pendingMutation);
        } else {
            return false;
        }
    }

    /**
     * Get the size of the queue.
     *
     * @return return the size of the queue
     */
    public int size() {
        return mutationMap.size();
    }

    /**
     * Remove an object from the queue if its a {@link PendingMutation}.
     *
     * @param removingObject the object
     * @return return true if remove is successful
     */
    public boolean remove(@Nullable Object removingObject) {
        if (removingObject instanceof PendingMutation) {
            return removeById(((PendingMutation) removingObject).getMutationId());
        } else {
            return false;
        }
    }

    /**
     * Clear the entire queue.
     */
    public synchronized void clear() {
        dummyHead.next = dummyTail;
        dummyTail.prev = dummyHead;
        mutationMap.clear();
    }

    /**
     * Get the first {@link PendingMutation} instance from the queue, if queue is empty, return null.
     *
     * @return return a {@link PendingMutation} or null
     */
    @Nullable
    public synchronized PendingMutation<? extends Model> peek() {
        return mutationMap.isEmpty() ? null : dummyHead.next.mutation;
    }

    /**
     * This is just a wrapper class for mutation, hold a reference to its next and
     * previous Node in case of an remove or update.
     */
    private class Node {
        private TimeBasedUuid id;
        private PendingMutation<? extends Model> mutation;
        private Node next;
        private Node prev;
    }
}
