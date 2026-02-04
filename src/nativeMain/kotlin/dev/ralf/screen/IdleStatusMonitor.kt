package dev.ralf.screen

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

interface IdleStatusMonitor {
  val isIdle: StateFlow<Boolean>

  suspend fun monitor(idleThreshold: Duration)
}
