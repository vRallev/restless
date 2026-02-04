package dev.ralf.screen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePowerStatusMonitor(initialValue: Boolean = false) : PowerStatusMonitor {
  private val _isOnCharger = MutableStateFlow(initialValue)
  override val isOnCharger: StateFlow<Boolean> = _isOnCharger

  fun setOnCharger(value: Boolean) {
    _isOnCharger.value = value
  }

  override suspend fun monitor() = Unit
}
