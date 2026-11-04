package dev.ralf.restless

import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTimer(initialTime: Duration = Duration.ZERO) : Timer {
  override val timeLeft: StateFlow<Duration>
    field = MutableStateFlow(initialTime)

  override val isRunning: StateFlow<Boolean>
    field = MutableStateFlow(false)

  var startCalled = false
    private set

  var stopCalled = false
    private set

  var lastSetTime: Duration? = null
    private set

  override fun start() {
    startCalled = true
    isRunning.value = true
  }

  override fun stop() {
    stopCalled = true
    isRunning.value = false
  }

  override fun setTime(duration: Duration) {
    lastSetTime = duration
    timeLeft.value = duration
  }
}
