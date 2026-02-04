package dev.ralf.restless

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AppKit.NSApplication
import platform.AppKit.NSEvent
import platform.AppKit.NSEventMaskLeftMouseDown
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSStatusBar
import platform.AppKit.NSStatusItem
import platform.AppKit.NSVariableStatusItemLength
import platform.AppKit.NSWorkspace
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.darwin.NSObject

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class MenuItemManagerImpl(private val modeController: ModeController) : MenuItemManager {
  private lateinit var statusItem: NSStatusItem
  private var cancelItem: NSMenuItem? = null

  // Store references to prevent garbage collection
  private var actionHandler: StatusBarActionHandler? = null
  private var eventMonitor: Any? = null

  override fun updateTitle(title: String) {
    if (::statusItem.isInitialized) {
      statusItem.button?.title = title
    }
  }

  override fun setManualMode(isManual: Boolean) {
    cancelItem?.setHidden(!isManual)
  }

  override suspend fun setupMenu() {
    withContext(Dispatchers.Main) {
      statusItem = NSStatusBar.systemStatusBar.statusItemWithLength(NSVariableStatusItemLength)

      val menu = NSMenu()

      // Cancel item (hidden by default, shown only in manual mode)
      val cancelMenuItem = NSMenuItem("Cancel", NSSelectorFromString("cancelAction"), "")
      cancelMenuItem.setHidden(true)
      menu.addItem(cancelMenuItem)
      cancelItem = cancelMenuItem

      // About item
      val aboutItem = NSMenuItem("About Restless", NSSelectorFromString("openAbout"), "")
      menu.addItem(aboutItem)

      // Separator (always visible, separates Cancel from Exit)
      menu.addItem(NSMenuItem.separatorItem())

      // Exit item
      val exitItem = NSMenuItem("Exit", NSSelectorFromString("exitApp"), "")
      menu.addItem(exitItem)

      // Create action handler for menu items
      actionHandler =
        StatusBarActionHandler(
          onCancel = { modeController.cancelManualMode() },
          onAbout = {
            val url = NSURL.URLWithString("https://github.com/vRallev/restless")
            if (url != null) {
              NSWorkspace.sharedWorkspace.openURL(url)
            }
          },
          onExit = { NSApplication.sharedApplication.terminate(null) },
        )

      // Set targets for menu items
      cancelMenuItem.target = actionHandler
      aboutItem.target = actionHandler
      exitItem.target = actionHandler

      // Assign menu to status item - this handles right-click automatically
      statusItem.menu = menu

      // Set up event monitor to intercept left-clicks on the status bar button
      eventMonitor =
        NSEvent.addLocalMonitorForEventsMatchingMask(NSEventMaskLeftMouseDown) { event ->
          if (event != null && isClickOnStatusItem(event)) {
            modeController.onStatusBarClick()
            null // Consume the event (don't show menu on left-click)
          } else {
            event // Pass through
          }
        }
    }
  }

  private fun isClickOnStatusItem(event: NSEvent): Boolean {
    val button = statusItem.button ?: return false
    val window = button.window ?: return false

    // Check if the event is in our status item's window
    if (event.window != window) return false

    // The event is in our status bar button's window, so it's clicking on us
    return true
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  private class StatusBarActionHandler(
    private val onCancel: () -> Unit,
    private val onAbout: () -> Unit,
    private val onExit: () -> Unit,
  ) : NSObject() {

    @ObjCAction
    fun cancelAction() {
      onCancel()
    }

    @ObjCAction
    fun openAbout() {
      onAbout()
    }

    @ObjCAction
    fun exitApp() {
      onExit()
    }
  }
}
