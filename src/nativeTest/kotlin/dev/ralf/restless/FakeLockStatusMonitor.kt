package dev.ralf.restless

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeLockStatusMonitor(initialValue: Boolean = false) : LockStatusMonitor {
  override val isLocked: StateFlow<Boolean>
    field = MutableStateFlow(initialValue)

  fun setLocked(value: Boolean) {
    isLocked.value = value
  }

  override suspend fun monitor() = Unit
}
