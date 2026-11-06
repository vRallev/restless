package dev.ralf.restless

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.AppKit.NSApplication
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSButton
import platform.AppKit.NSColor
import platform.AppKit.NSLayoutAttributeFirstBaseline
import platform.AppKit.NSLayoutAttributeLeading
import platform.AppKit.NSLayoutConstraint
import platform.AppKit.NSStackView
import platform.AppKit.NSTextField
import platform.AppKit.NSUserInterfaceLayoutOrientationVertical
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.currentEditor
import platform.AppKit.labelWithString
import platform.AppKit.textFieldWithString
import platform.Foundation.NSMakeRect
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

@Inject
@SingleIn(AppScope::class)
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class SettingsWindow(private val settings: Settings) : NSObject() {
  private val timeoutField by lazy {
    NSTextField.textFieldWithString("").apply {
      setAccessibilityLabel("Automatic timeout on power in minutes")
      widthAnchor.constraintEqualToConstant(60.0).active = true
    }
  }
  private val messageLabel by lazy { NSTextField.labelWithString("") }
  private val window by lazy { createWindow() }

  fun show() {
    val isOpening = !window.visible
    if (isOpening) {
      timeoutField.stringValue = settings.powerTimeoutMinutes.toString()
      messageLabel.stringValue = "Applies to the next automatic timer."
      messageLabel.textColor = NSColor.secondaryLabelColor
    }

    NSApplication.sharedApplication.activateIgnoringOtherApps(true)
    window.makeKeyAndOrderFront(null)
    if (isOpening) {
      timeoutField.selectText(null)
    }
  }

  @ObjCAction
  fun save() {
    val text = timeoutField.currentEditor()?.string ?: timeoutField.stringValue
    val minutes = text.trim().toLongOrNull()
    val range = Settings.powerTimeoutRange
    if (minutes == null || minutes !in range) {
      messageLabel.stringValue = "Enter a whole number from ${range.first} to ${range.last}."
      messageLabel.textColor = NSColor.systemRedColor
      timeoutField.selectText(null)
      return
    }

    settings.powerTimeoutMinutes = minutes
    window.close()
  }

  private fun createWindow(): NSWindow {
    val settingsWindow =
      NSWindow(
        contentRect = NSMakeRect(x = 0.0, y = 0.0, w = 360.0, h = 126.0),
        styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskClosable,
        backing = NSBackingStoreBuffered,
        defer = false,
      ).apply {
        title = "Restless Settings"
        releasedWhenClosed = false
        center()
      }

    val timeoutRow =
      NSStackView.stackViewWithViews(
        listOf(
          NSTextField.labelWithString("Automatic timeout on power:"),
          timeoutField,
          NSTextField.labelWithString("minutes"),
        )
      ).apply { alignment = NSLayoutAttributeFirstBaseline }

    val form =
      NSStackView.stackViewWithViews(listOf(timeoutRow, messageLabel)).apply {
        orientation = NSUserInterfaceLayoutOrientationVertical
        alignment = NSLayoutAttributeLeading
        translatesAutoresizingMaskIntoConstraints = false
      }

    val cancelButton =
      NSButton.buttonWithTitle(
        title = "Cancel",
        target = settingsWindow,
        action = NSSelectorFromString("performClose:"),
      ).apply { keyEquivalent = "\u001b" }
    val saveButton =
      NSButton.buttonWithTitle(title = "Save", target = this, action = NSSelectorFromString("save"))
        .apply { keyEquivalent = "\r" }
    val buttons =
      NSStackView.stackViewWithViews(listOf(cancelButton, saveButton)).apply {
        translatesAutoresizingMaskIntoConstraints = false
      }

    val content = checkNotNull(settingsWindow.contentView)
    content.addSubview(form)
    content.addSubview(buttons)
    NSLayoutConstraint.activateConstraints(
      listOf(
        form.leadingAnchor.constraintEqualToAnchor(anchor = content.leadingAnchor, constant = 20.0),
        form.topAnchor.constraintEqualToAnchor(anchor = content.topAnchor, constant = 20.0),
        buttons.trailingAnchor.constraintEqualToAnchor(
          anchor = content.trailingAnchor,
          constant = -20.0,
        ),
        buttons.bottomAnchor.constraintEqualToAnchor(anchor = content.bottomAnchor, constant = -20.0),
      )
    )
    return settingsWindow
  }
}
