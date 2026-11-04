package dev.ralf.restless

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TimerImpl(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) : Timer {
  private var countdownJob: Job? = null

  override val timeLeft: StateFlow<Duration>
    field = MutableStateFlow(Duration.ZERO)

  override val isRunning: StateFlow<Boolean>
    field = MutableStateFlow(false)

  override fun start() {
    if (isRunning.value) {
      return
    }

    isRunning.value = true
    countdownJob = scope.launch {
      while (timeLeft.value > Duration.ZERO) {
        delay(1.seconds)
        timeLeft.value -= 1.seconds
      }
      println("DEBUG: Timer reached 0")
      isRunning.value = false
    }
  }

  override fun stop() {
    countdownJob?.cancel()
    countdownJob = null
    isRunning.value = false
  }

  override fun setTime(duration: Duration) {
    timeLeft.value = duration
  }
}
