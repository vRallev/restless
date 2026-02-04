package dev.ralf.restless

import kotlinx.coroutines.flow.StateFlow

interface LockStatusMonitor {
  val isLocked: StateFlow<Boolean>

  suspend fun monitor()
}
