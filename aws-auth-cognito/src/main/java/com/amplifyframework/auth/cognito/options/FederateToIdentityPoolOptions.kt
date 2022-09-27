package com.amplifyframework.auth.cognito.options

/**
 * Options to provide additional fields to federate to Cognito Identity Provider.
 */
data class FederateToIdentityPoolOptions internal constructor(val developerProvidedIdentityId: String?) {

    /**
     * The builder for this class.
     */
    class Builder {
        private var developerProvidedIdentityId: String? = null

        /**
         * Set the developerProvidedIdentityId field for the object being built.
         * @param _developerProvidedIdentityId Provide identity id for federation to Cognito Identity Provider
         * @return The builder object to continue building.
         */
        fun developerProvidedIdentityId(_developerProvidedIdentityId: String): Builder {
            this.developerProvidedIdentityId = _developerProvidedIdentityId
            return this
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of FederateToIdentityPoolOptions with the values specified in the builder.
         */
        fun build(): FederateToIdentityPoolOptions {
            return FederateToIdentityPoolOptions(developerProvidedIdentityId)
        }
    }

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
