package dev.ralf.restless

import kotlin.time.Duration

class FakeCaffeinate : Caffeinate {
  var startCalled = false
    private set

  var stopCalled = false
    private set

  var lastDuration: Duration? = null
    private set

  override fun start(duration: Duration) {
    startCalled = true
    lastDuration = duration
  }

  override fun stop() {
    stopCalled = true
  }
}
