package dev.ralf.restless

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

interface Timer {
  val timeLeft: StateFlow<Duration>
  val isRunning: StateFlow<Boolean>

  fun start()

  fun stop()

  fun setTime(duration: Duration)
}
