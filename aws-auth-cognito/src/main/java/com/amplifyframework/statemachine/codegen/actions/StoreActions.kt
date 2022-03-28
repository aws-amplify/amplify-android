package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential

interface StoreActions {
    fun migrateLegacyCredentialStoreAction(): Action
    fun clearCredentialStoreAction(): Action
    fun loadCredentialStoreAction(): Action
    fun storeCredentialsAction(credentials: AmplifyCredential): Action
    fun moveToIdleStateAction(): Action
}
