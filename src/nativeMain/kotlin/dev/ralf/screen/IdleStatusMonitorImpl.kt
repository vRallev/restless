package dev.ralf.screen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import platform.CoreGraphics.CGEventSourceSecondsSinceLastEventType
import platform.CoreGraphics.kCGEventSourceStateCombinedSessionState

private const val kCGAnyInputEventType = 0xFFFFFFFFu

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IdleStatusMonitorImpl : IdleStatusMonitor {
  private val _isIdle = MutableStateFlow(false)
  override val isIdle: StateFlow<Boolean> = _isIdle.asStateFlow()

  override suspend fun monitor(idleThreshold: Duration) {
    println("DEBUG: Idle status monitoring started (threshold: $idleThreshold)")

    try {
      while (currentCoroutineContext().isActive) {
        val idleSeconds =
          CGEventSourceSecondsSinceLastEventType(
            kCGEventSourceStateCombinedSessionState,
            kCGAnyInputEventType,
          )
        val newIsIdle = idleSeconds >= idleThreshold.inWholeSeconds

        if (_isIdle.value != newIsIdle) {
          _isIdle.value = newIsIdle
          println("DEBUG: Idle state changed, isIdle = $newIsIdle (idle for ${idleSeconds}s)")
        }

        delay(1.seconds)
      }
    } finally {
      println("DEBUG: Idle status monitoring stopped")
    }
  }
}
