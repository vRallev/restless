package dev.ralf.screen

import kotlin.time.Duration

interface Caffeinate {
  fun start(duration: Duration)

  fun stop()
}
