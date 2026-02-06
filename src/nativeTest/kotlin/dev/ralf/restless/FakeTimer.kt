package dev.ralf.restless

import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTimer(initialTime: Duration = Duration.ZERO) : Timer {
  private val _timeLeft = MutableStateFlow(initialTime)
  override val timeLeft: StateFlow<Duration> = _timeLeft

  private val _isRunning = MutableStateFlow(false)
  override val isRunning: StateFlow<Boolean> = _isRunning

  var startCalled = false
    private set

  var stopCalled = false
    private set

  var lastSetTime: Duration? = null
    private set

  override fun start() {
    startCalled = true
    _isRunning.value = true
  }

  override fun stop() {
    stopCalled = true
    _isRunning.value = false
  }

  override fun setTime(duration: Duration) {
    lastSetTime = duration
    _timeLeft.value = duration
  }
}
