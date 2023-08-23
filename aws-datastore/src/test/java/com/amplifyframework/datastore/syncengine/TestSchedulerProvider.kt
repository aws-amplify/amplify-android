package com.amplifyframework.datastore.syncengine

import io.reactivex.rxjava3.schedulers.TestScheduler

class TestSchedulerProvider(private val scheduler: TestScheduler) : SchedulerProvider {
    override fun computation() = scheduler
    override fun io() = scheduler
}
