package dev.ralf.restless

import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeIdleStatusMonitor(initialValue: Boolean = false) : IdleStatusMonitor {
  override val isIdle: StateFlow<Boolean>
    field = MutableStateFlow(initialValue)

  fun setIdle(value: Boolean) {
    isIdle.value = value
  }

  override suspend fun monitor(idleThreshold: Duration) = Unit
}
