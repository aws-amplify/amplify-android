package com.amplifyframework.auth.options

import androidx.core.util.ObjectsCompat

/**
 * The shared options among all Auth plugins.
 * @param forceRefresh force refresh credentials even if they are not expired.
 */
open class AuthFetchSessionOptions protected constructor(val forceRefresh: Boolean) {
    companion object {
        /**
         * Use the default fetch auth session options. By default force refresh is false.
         * @return Default fetch auth session options.
         */
        @JvmStatic
        fun defaults() = AuthFetchSessionOptions(false)

        /**
         * Get a builder to construct an instance of this object.
         * @return a builder to construct an instance of this object.
         */
        @JvmStatic
        fun builder(): Builder<*> = CoreBuilder()
    }

    /**
     * When overriding, be sure to include forceRefresh in the hash.
     * @return Hash code of this object
     */
    override fun hashCode() = ObjectsCompat.hash(forceRefresh)

    /**
     * When overriding, be sure to include forceRefresh in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other == null || javaClass != other.javaClass) {
            false
        } else {
            val authFetchSessionOptions = other as AuthFetchSessionOptions
            ObjectsCompat.equals(forceRefresh, authFetchSessionOptions.forceRefresh)
        }
    }

    /**
     * When overriding, be sure to include forceRefresh in the output string.
     * @return A string representation of the object
     */
    override fun toString(): String {
        return "AuthFetchSessionOptions{" +
            "forceRefresh=" + forceRefresh +
            '}'
    }

    /**
     * The builder for this class.
     * @param <T> The type of builder - used to support plugin extensions of this.
     */
    abstract class Builder<T : Builder<T>> {
        /**
         * Specifics if Amplify should force refresh credentials even if they are not expired.
         * @return True if it should force refresh credentials and False if it should not force refresh credentials
         */
        var forceRefresh = false
            private set

        /**
         * Return the type of builder this is so that chaining can work correctly without implicit casting.
         * @return the type of builder this is
         */
        abstract fun getThis(): T

        /**
         * Specifics if Amplify should force refresh credentials even if they are not expired.
         * @param forceRefresh True if it should force refresh credentials and False if it should not force refresh credentials
         * @return The builder object
         */
        open fun forceRefresh(forceRefresh: Boolean) = apply {
            this.forceRefresh = forceRefresh
        }

        /**
         * Build an instance of AuthFetchSessionOptions (or one of its subclasses).
         * @return an instance of AuthFetchSessionOptions (or one of its subclasses)
         */
        open fun build() = AuthFetchSessionOptions(forceRefresh)
    }

    /**
     * The specific implementation of builder for this as the parent class.
     */
    class CoreBuilder : Builder<CoreBuilder>() {
        override fun getThis() = this
    }
}
