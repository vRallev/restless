package dev.ralf.screen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRunLoopAddSource
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopRemoveSource
import platform.CoreFoundation.CFRunLoopSourceRef
import platform.CoreFoundation.CFStringCompare
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFCompareEqualTo
import platform.CoreFoundation.kCFRunLoopDefaultMode
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.IOKit.IOPSCopyPowerSourcesInfo
import platform.IOKit.IOPSCopyPowerSourcesList
import platform.IOKit.IOPSGetPowerSourceDescription
import platform.IOKit.IOPSNotificationCreateRunLoopSource

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class PowerStatusMonitorImpl : PowerStatusMonitor {
  private val _isOnCharger = MutableStateFlow(checkIsOnAcPower())

  override val isOnCharger: StateFlow<Boolean> = _isOnCharger.asStateFlow()

  override suspend fun monitor() {
    instance = this
    val runLoopSource: CFRunLoopSourceRef =
      checkNotNull(
        IOPSNotificationCreateRunLoopSource(
          staticCFunction { _ -> instance?.onPowerSourceChanged() },
          null,
        )
      )

    CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource, kCFRunLoopDefaultMode)
    println("DEBUG: Power source monitoring started")

    try {
      awaitCancellation()
    } finally {
      CFRunLoopRemoveSource(CFRunLoopGetCurrent(), runLoopSource, kCFRunLoopDefaultMode)
      CFRelease(runLoopSource)
      instance = null
      println("DEBUG: Power source monitoring stopped")
    }
  }

  private fun onPowerSourceChanged() {
    val newValue = checkIsOnAcPower()
    println("DEBUG: Power source changed, onAcPower = $newValue")
    _isOnCharger.value = newValue
  }

  private fun checkIsOnAcPower(): Boolean {
    val powerInfo =
      IOPSCopyPowerSourcesInfo()
        ?: run {
          println("DEBUG: powerInfo is null")
          return false
        }
    val powerSources =
      IOPSCopyPowerSourcesList(powerInfo)
        ?: run {
          println("DEBUG: powerSources is null")
          CFRelease(powerInfo)
          return false
        }

    val count = CFArrayGetCount(powerSources)
    println("DEBUG: power source count = $count")
    var onAcPower = false

    val powerSourceStateKey: CFStringRef =
      CFStringCreateWithCString(kCFAllocatorDefault, "Power Source State", kCFStringEncodingUTF8)
        ?: run {
          CFRelease(powerSources)
          CFRelease(powerInfo)
          return false
        }

    val acPowerValue: CFStringRef =
      CFStringCreateWithCString(kCFAllocatorDefault, "AC Power", kCFStringEncodingUTF8)
        ?: run {
          CFRelease(powerSourceStateKey)
          CFRelease(powerSources)
          CFRelease(powerInfo)
          return false
        }

    for (i in 0 until count) {
      val source = CFArrayGetValueAtIndex(powerSources, i) ?: continue
      val description: CFDictionaryRef =
        IOPSGetPowerSourceDescription(powerInfo, source) ?: continue

      val powerStateValue = CFDictionaryGetValue(description, powerSourceStateKey)
      println("DEBUG: powerStateValue = $powerStateValue")
      if (powerStateValue != null) {
        val stateString: CFStringRef = powerStateValue.reinterpret()
        val isAcPower = CFStringCompare(stateString, acPowerValue, 0u) == kCFCompareEqualTo
        println("DEBUG: isAcPower = $isAcPower")
        if (isAcPower) {
          onAcPower = true
          break
        }
      }
    }

    CFRelease(acPowerValue)
    CFRelease(powerSourceStateKey)
    CFRelease(powerSources)
    CFRelease(powerInfo)
    println("DEBUG: returning onAcPower = $onAcPower")
    return onAcPower
  }

  companion object {
    private var instance: PowerStatusMonitorImpl? = null
  }
}
