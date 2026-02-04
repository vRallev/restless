package dev.ralf.screen

import kotlinx.coroutines.flow.StateFlow

enum class AppMode {
  AUTOMATIC,
  MANUAL,
}

interface ModeController {
  val mode: StateFlow<AppMode>

  /** Called when user left-clicks status bar. Switches to manual mode and adds 1 hour. */
  fun onStatusBarClick()

  /** Called when user clicks Cancel in menu. Stops timer and returns to automatic mode. */
  fun cancelManualMode()
}
