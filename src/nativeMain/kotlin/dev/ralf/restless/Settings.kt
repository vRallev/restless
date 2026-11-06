package dev.ralf.restless

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@Inject
@SingleIn(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class Settings(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) {
  var powerTimeoutMinutes: Long
    get() =
      defaults.integerForKey(POWER_TIMEOUT_MINUTES_KEY).takeIf { it in powerTimeoutRange } ?: 120L
    set(value) {
      require(value in powerTimeoutRange)
      defaults.setInteger(value = value, forKey = POWER_TIMEOUT_MINUTES_KEY)
    }

  companion object {
    val powerTimeoutRange = 1L..1440L

    private const val POWER_TIMEOUT_MINUTES_KEY = "powerTimeoutMinutes"
  }
}
