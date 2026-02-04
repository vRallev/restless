package dev.ralf.restless

import kotlinx.coroutines.flow.StateFlow

interface PowerStatusMonitor {
  val isOnCharger: StateFlow<Boolean>

  suspend fun monitor()
}
