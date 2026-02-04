package dev.ralf.screen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.getpid
import platform.posix.pclose
import platform.posix.popen

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class CaffeinateImpl : Caffeinate {
  private val appPid = getpid()

  override fun start(duration: Duration) {
    // Stop any existing caffeinate for our app first
    stop()

    val seconds = duration.inWholeSeconds

    // Launch caffeinate in background
    // -w watches our PID so it exits if we crash
    // -t is a backup timeout
    val command = "/usr/bin/caffeinate -dimsu -w $appPid -t $seconds &"
    val pipe = popen(command, "r")
    if (pipe != null) {
      pclose(pipe)
      println("DEBUG: Caffeinate started (appPid=$appPid, timeout=${seconds}s)")
    } else {
      println("DEBUG: Failed to start caffeinate")
    }
  }

  override fun stop() {
    // Find and kill only the caffeinate processes watching our PID
    val findCommand = "pgrep -f 'caffeinate.*-w $appPid' 2>/dev/null"
    val findPipe = popen(findCommand, "r")
    if (findPipe != null) {
      val buffer = ByteArray(64)
      val output = StringBuilder()
      while (true) {
        val line = fgets(buffer.refTo(0), buffer.size, findPipe) ?: break
        output.append(buffer.toKString())
      }
      pclose(findPipe)

      val pids = output.toString().trim().lines().mapNotNull { it.trim().toIntOrNull() }
      if (pids.isNotEmpty()) {
        val killCommand = "kill ${pids.joinToString(" ")} 2>/dev/null"
        val killPipe = popen(killCommand, "r")
        if (killPipe != null) {
          pclose(killPipe)
        }
        println("DEBUG: Caffeinate stopped")
      }
    }
  }
}
