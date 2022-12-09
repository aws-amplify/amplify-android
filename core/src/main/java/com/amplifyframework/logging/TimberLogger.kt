package com.amplifyframework.logging

import timber.log.Timber

/**
 * weiping@atlasv.com
 * 2022/12/9
 */
class TimberLogger(private val namespace: String, private val threshold: LogLevel) : Logger {
    override fun getThresholdLevel(): LogLevel {
        return threshold
    }

    override fun getNamespace(): String {
        return namespace
    }

    override fun error(message: String?) {
        if (threshold.above(LogLevel.ERROR)) {
            return
        }
        Timber.tag(namespace).e { message }
    }

    override fun error(message: String?, error: Throwable?) {
        if (threshold.above(LogLevel.ERROR)) {
            return
        }
        Timber.tag(namespace).e(error) { message }
    }

    override fun warn(message: String?) {
        if (threshold.above(LogLevel.WARN)) {
            return
        }
        Timber.tag(namespace).w { message }
    }

    override fun warn(message: String?, issue: Throwable?) {
        if (threshold.above(LogLevel.WARN)) {
            return
        }
        Timber.tag(namespace).w(issue) { message }
    }

    override fun info(message: String?) {
        if (threshold.above(LogLevel.INFO)) {
            return
        }
        Timber.tag(namespace).d { message }
    }

    override fun debug(message: String?) {
        if (threshold.above(LogLevel.DEBUG)) {
            return
        }
        Timber.tag(namespace).d { message }
    }

    override fun verbose(message: String?) {
        if (threshold.above(LogLevel.VERBOSE)) {
            return
        }
        Timber.tag(namespace).d { message }
    }
}