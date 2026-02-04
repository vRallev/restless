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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TimerImpl(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) : Timer {
  private var countdownJob: Job? = null

  private val _timeLeft = MutableStateFlow(Duration.ZERO)
  override val timeLeft: StateFlow<Duration> = _timeLeft.asStateFlow()

  private val _isRunning = MutableStateFlow(false)
  override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

  override fun start() {
    if (_isRunning.value) return

    _isRunning.value = true
    countdownJob =
      scope.launch {
        while (_timeLeft.value > Duration.ZERO) {
          delay(1.seconds)
          _timeLeft.value -= 1.seconds
        }
        println("DEBUG: Timer reached 0")
        _isRunning.value = false
      }
  }

  override fun stop() {
    countdownJob?.cancel()
    countdownJob = null
    _isRunning.value = false
  }

  override fun setTime(duration: Duration) {
    _timeLeft.value = duration
  }
}
