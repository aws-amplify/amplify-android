package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action

interface DeleteUserActions {
    fun initDeleteUserAction(accessToken: String): Action
}
