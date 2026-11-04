package dev.ralf.restless

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
import platform.Foundation.NSDistributedNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class LockStatusMonitorImpl : LockStatusMonitor {
  override val isLocked: StateFlow<Boolean>
    field = MutableStateFlow(false)

  private val observer =
    LockStatusObserver(
      onLocked = { isLocked.value = true },
      onUnlocked = { isLocked.value = false },
    )

  override suspend fun monitor() {
    val center = NSDistributedNotificationCenter.defaultCenter

    center.addObserver(
      observer = observer,
      selector = NSSelectorFromString("onDisplayLocked:"),
      name = DISPLAY_LOCKED,
      `object` = null,
    )

    center.addObserver(
      observer = observer,
      selector = NSSelectorFromString("onDisplayUnlocked:"),
      name = DISPLAY_UNLOCKED,
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
    const val DISPLAY_LOCKED = "com.apple.screenIsLocked"
    const val DISPLAY_UNLOCKED = "com.apple.screenIsUnlocked"
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  private class LockStatusObserver(
    private val onLocked: () -> Unit,
    private val onUnlocked: () -> Unit,
  ) : NSObject() {

    @ObjCAction
    fun onDisplayLocked(notification: NSNotification?) {
      println("DEBUG: Display locked")
      onLocked()
    }

    @ObjCAction
    fun onDisplayUnlocked(notification: NSNotification?) {
      println("DEBUG: Display unlocked")
      onUnlocked()
    }
  }
}
