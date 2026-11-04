package dev.ralf.restless

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePowerStatusMonitor(initialValue: Boolean = false) : PowerStatusMonitor {
  override val isOnCharger: StateFlow<Boolean>
    field = MutableStateFlow(initialValue)

  fun setOnCharger(value: Boolean) {
    isOnCharger.value = value
  }

  override suspend fun monitor() = Unit
}
