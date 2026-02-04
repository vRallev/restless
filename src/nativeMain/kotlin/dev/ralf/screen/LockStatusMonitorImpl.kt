package dev.ralf.screen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSDistributedNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class LockStatusMonitorImpl : LockStatusMonitor {
  private val _isLocked = MutableStateFlow(false)
  override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

  private val observer =
    LockStatusObserver(
      onLocked = { _isLocked.value = true },
      onUnlocked = { _isLocked.value = false },
    )

  override suspend fun monitor() {
    val center = NSDistributedNotificationCenter.defaultCenter

    center.addObserver(
      observer = observer,
      selector = NSSelectorFromString("onScreenLocked:"),
      name = SCREEN_LOCKED,
      `object` = null,
    )

    center.addObserver(
      observer = observer,
      selector = NSSelectorFromString("onScreenUnlocked:"),
      name = SCREEN_UNLOCKED,
      `object` = null,
    )

    println("DEBUG: Lock status monitoring started")

    try {
      awaitCancellation()
    } finally {
      center.removeObserver(observer)
      println("DEBUG: Lock status monitoring stopped")
    }
  }

  private companion object {
    const val SCREEN_LOCKED = "com.apple.screenIsLocked"
    const val SCREEN_UNLOCKED = "com.apple.screenIsUnlocked"
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  private class LockStatusObserver(
    private val onLocked: () -> Unit,
    private val onUnlocked: () -> Unit,
  ) : NSObject() {

    @ObjCAction
    fun onScreenLocked(notification: NSNotification?) {
      println("DEBUG: Screen locked")
      onLocked()
    }

    @ObjCAction
    fun onScreenUnlocked(notification: NSNotification?) {
      println("DEBUG: Screen unlocked")
      onUnlocked()
    }
  }
}
