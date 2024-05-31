package featureTest.utilities

import java.util.TimeZone
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TimeZoneRule(private val timeZone: TimeZone) : TestWatcher() {
    private val previous: TimeZone = TimeZone.getDefault()

    override fun starting(description: Description) {
        TimeZone.setDefault(timeZone)
    }

    override fun finished(description: Description) {
        TimeZone.setDefault(previous)
    }
}
