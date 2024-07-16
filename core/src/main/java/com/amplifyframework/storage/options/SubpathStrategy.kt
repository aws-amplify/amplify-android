package com.amplifyframework.storage.options

sealed class SubpathStrategy {
    data object Include: SubpathStrategy()
    data class Exclude(val delimiter: String = "/"): SubpathStrategy()
}