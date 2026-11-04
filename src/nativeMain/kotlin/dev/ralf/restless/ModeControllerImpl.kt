package dev.ralf.restless

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModeControllerImpl(private val timer: Timer, private val caffeinate: Caffeinate) :
  ModeController {

  override val mode: StateFlow<AppMode>
    field = MutableStateFlow(AppMode.AUTOMATIC)

  override fun onStatusBarClick() {
    val currentTime = timer.timeLeft.value
    val newTime = (currentTime + 1.hours).coerceAtMost(24.hours)

    timer.setTime(newTime)

    if (mode.value == AppMode.AUTOMATIC) {
      mode.value = AppMode.MANUAL
    }

    if (!timer.isRunning.value) {
      timer.start()
    }
    // Update caffeinate with new duration
    caffeinate.stop()
    caffeinate.start(newTime)
  }

  override fun cancelManualMode() {
    mode.value = AppMode.AUTOMATIC
    timer.stop()
    timer.setTime(Duration.ZERO)
    caffeinate.stop()
  }
}
