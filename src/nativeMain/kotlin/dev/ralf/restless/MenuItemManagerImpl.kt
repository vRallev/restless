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
import platform.AppKit.NSClickGestureRecognizer
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
class MenuItemManagerImpl(
  private val modeController: ModeController,
  private val settingsWindow: SettingsWindow,
) : MenuItemManager {
  private lateinit var statusItem: NSStatusItem
  private var cancelItem: NSMenuItem? = null

  // AppKit holds action targets weakly.
  private lateinit var actionHandler: StatusBarActionHandler

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

      val settingsItem =
        NSMenuItem(
          title = "Settings…",
          action = NSSelectorFromString("openSettings"),
          keyEquivalent = ",",
        )
      menu.addItem(settingsItem)

      menu.addItem(NSMenuItem.separatorItem())

      // Exit item
      val exitItem = NSMenuItem("Exit", NSSelectorFromString("exitApp"), "")
      menu.addItem(exitItem)

      actionHandler =
        StatusBarActionHandler(
          onPrimaryClick = { modeController.onStatusBarClick() },
          onSecondaryClick = {
            // An attached status item menu takes over primary clicks.
            statusItem.menu = menu
            try {
              statusItem.button?.performClick(null)
            } finally {
              statusItem.menu = null
            }
          },
          onCancel = { modeController.cancelManualMode() },
          onAbout = {
            val url = NSURL.URLWithString("https://github.com/vRallev/restless")
            if (url != null) {
              NSWorkspace.sharedWorkspace.openURL(url)
            }
          },
          onSettings = { settingsWindow.show() },
          onExit = { NSApplication.sharedApplication.terminate(null) },
        )

      // Set targets for menu items
      cancelMenuItem.target = actionHandler
      aboutItem.target = actionHandler
      settingsItem.target = actionHandler
      exitItem.target = actionHandler

      statusItem.button?.apply {
        target = actionHandler
        action = NSSelectorFromString("addHour")
        val rightClick =
          NSClickGestureRecognizer(
            target = actionHandler,
            action = NSSelectorFromString("showMenu"),
          )
        rightClick.buttonMask = 0x2uL
        addGestureRecognizer(rightClick)
      }
    }
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  private class StatusBarActionHandler(
    private val onPrimaryClick: () -> Unit,
    private val onSecondaryClick: () -> Unit,
    private val onCancel: () -> Unit,
    private val onAbout: () -> Unit,
    private val onSettings: () -> Unit,
    private val onExit: () -> Unit,
  ) : NSObject() {

    @ObjCAction
    fun addHour() {
      onPrimaryClick()
    }

    @ObjCAction
    fun showMenu() {
      onSecondaryClick()
    }

    @ObjCAction
    fun cancelAction() {
      onCancel()
    }

    @ObjCAction
    fun openAbout() {
      onAbout()
    }

    @ObjCAction
    fun openSettings() {
      onSettings()
    }

    @ObjCAction
    fun exitApp() {
      onExit()
    }
  }
}
