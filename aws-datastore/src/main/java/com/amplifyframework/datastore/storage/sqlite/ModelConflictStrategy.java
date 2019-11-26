/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.amplifyframework.datastore.storage.sqlite;

/**
 * The strategy to be used when there is an existing model with the same ID.
 */
enum ModelConflictStrategy {
    /**
     * Ignore the existing model. Just overwrite (update) it.
     * This may be the appropriate strategy for an update to an existing item.
     */
    OVERWRITE_EXISTING,

    /**
     * This situation constitutes an error, so we should throw an exception.
     * This may be the appropriate strategy when inserting a newly created model instance.
     */
    THROW_EXCEPTION
}
