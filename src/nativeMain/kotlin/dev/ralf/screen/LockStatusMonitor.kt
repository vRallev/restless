package dev.ralf.screen

import kotlinx.coroutines.flow.StateFlow

interface LockStatusMonitor {
  val isLocked: StateFlow<Boolean>

  suspend fun monitor()
}
