package com.amplifyframework.statemachine

internal interface Builder<T> {
    fun build(): T
}