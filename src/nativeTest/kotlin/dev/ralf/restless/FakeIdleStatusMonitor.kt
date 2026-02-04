package dev.ralf.restless

import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeIdleStatusMonitor(initialValue: Boolean = false) : IdleStatusMonitor {
  private val _isIdle = MutableStateFlow(initialValue)
  override val isIdle: StateFlow<Boolean> = _isIdle

  fun setIdle(value: Boolean) {
    _isIdle.value = value
  }

  override suspend fun monitor(idleThreshold: Duration) = Unit
}
