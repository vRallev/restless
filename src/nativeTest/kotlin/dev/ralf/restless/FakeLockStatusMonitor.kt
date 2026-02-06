package dev.ralf.restless

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeLockStatusMonitor(initialValue: Boolean = false) : LockStatusMonitor {
  private val _isLocked = MutableStateFlow(initialValue)
  override val isLocked: StateFlow<Boolean> = _isLocked

  fun setLocked(value: Boolean) {
    _isLocked.value = value
  }

  override suspend fun monitor() = Unit
}
