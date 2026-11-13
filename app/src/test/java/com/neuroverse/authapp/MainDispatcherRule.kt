package com.neuroverse.authapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that replaces `Dispatchers.Main` with a test dispatcher.
 *
 * WHY THIS IS NEEDED: `viewModelScope` runs on `Dispatchers.Main`, which is
 * backed by Android's main Looper. In a plain JVM unit test there is no Looper,
 * so every ViewModel test would fail with
 * "Module with the Main dispatcher had failed to initialize".
 *
 * [StandardTestDispatcher] also gives the test CONTROL over time: coroutines
 * queue until `advanceUntilIdle()` is called, so a test can assert the loading
 * state mid-flight and then let the call finish. That is impossible with a real
 * dispatcher, where timing is a race.
 *
 * Java comparison: a JUnit 4 @Rule is the same idea as a @Before/@After pair,
 * packaged so it can be reused across test classes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        // Always undo it: leaving Main replaced would leak into other test
        // classes and cause failures that depend on test execution order.
        Dispatchers.resetMain()
    }
}
