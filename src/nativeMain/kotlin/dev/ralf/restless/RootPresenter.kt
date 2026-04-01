package dev.ralf.restless

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Inject
@SingleIn(AppScope::class)
class RootPresenter(
  private val powerStatusMonitor: PowerStatusMonitor,
  private val idleStatusMonitor: IdleStatusMonitor,
  private val lockStatusMonitor: LockStatusMonitor,
  private val timer: Timer,
  private val caffeinate: Caffeinate,
  private val modeController: ModeController,
) : Presenter {

  @Composable
  override fun present(): Model {
    val isOnCharger by powerStatusMonitor.isOnCharger.collectAsState()
    val isIdle by idleStatusMonitor.isIdle.collectAsState()
    val isLocked by lockStatusMonitor.isLocked.collectAsState()
    val timeLeft by timer.timeLeft.collectAsState()
    val isRunning by timer.isRunning.collectAsState()
    val mode by modeController.mode.collectAsState()

    var showColon by remember { mutableStateOf(true) }

    // Blink colon every second only when timer is running
    LaunchedEffect(isRunning) {
      if (isRunning) {
        while (true) {
          delay(1.seconds)
          showColon = !showColon
        }
      } else {
        showColon = true
      }
    }

    // AUTOMATIC mode: Start timer when idle + on charger + not locked
    // MANUAL mode: Don't react to system state changes (timer controlled by clicks)
    LaunchedEffect(isIdle, isOnCharger, isLocked, mode) {
      if (mode == AppMode.AUTOMATIC) {
        if (isIdle && isOnCharger && !isLocked) {
          println("DEBUG: Starting timer (idle=$isIdle, onCharger=$isOnCharger, locked=$isLocked)")
          val duration = 2.hours
          timer.setTime(duration)
          timer.start()
          caffeinate.start(duration)
        } else {
          println("DEBUG: Stopping timer (idle=$isIdle, onCharger=$isOnCharger, locked=$isLocked)")
          timer.stop()
          timer.setTime(0.seconds)
          caffeinate.stop()
        }
      }
      // In MANUAL mode, do nothing - timer is controlled by clicks
    }

    // When a manual timer has already expired, leave manual mode only after some
    // external state changes again. This avoids immediately re-triggering
    // automatic mode while the Mac is still idle and about to sleep.
    LaunchedEffect(isIdle, isOnCharger, isLocked, mode, timeLeft, isRunning) {
      if (
        mode == AppMode.MANUAL &&
          timeLeft == 0.seconds &&
          !isRunning &&
          (!isIdle || !isOnCharger || isLocked)
      ) {
        println(
          "DEBUG: Resetting expired manual mode " +
            "(idle=$isIdle, onCharger=$isOnCharger, locked=$isLocked)",
        )
        modeController.cancelManualMode()
      }
    }

    // Log timer updates
    LaunchedEffect(timeLeft, isRunning, mode) {
      println("DEBUG: Timer update - timeLeft=$timeLeft, isRunning=$isRunning, mode=$mode")
    }

    val hours = timeLeft.inWholeHours.toString().padStart(2, '0')
    val minutes = (timeLeft.inWholeMinutes % 60).toString().padStart(2, '0')
    val separator = if (showColon) ":" else " "
    val prefix = if (mode == AppMode.MANUAL) "▶ " else ""
    val title = "$prefix$hours$separator$minutes"

    return Model(title = title, isManualMode = mode == AppMode.MANUAL)
  }

  data class Model(val title: String, val isManualMode: Boolean = false)
}
