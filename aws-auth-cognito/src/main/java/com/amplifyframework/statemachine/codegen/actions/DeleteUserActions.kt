package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action

internal interface DeleteUserActions {
    fun initDeleteUserAction(accessToken: String): Action
}
