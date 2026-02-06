package dev.ralf.restless

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

interface IdleStatusMonitor {
  val isIdle: StateFlow<Boolean>

  suspend fun monitor(idleThreshold: Duration)
}
