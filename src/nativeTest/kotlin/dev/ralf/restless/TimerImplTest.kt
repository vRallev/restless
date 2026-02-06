package dev.ralf.restless

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TimerImplTest {

  @Test
  fun `initial state has zero time and is not running`() = runTest {
    val timer = createTimer()

    assertThat(timer.timeLeft.value).isEqualTo(0.seconds)
    assertThat(timer.isRunning.value).isFalse()
  }

  @Test
  fun `setTime updates timeLeft`() = runTest {
    val timer = createTimer()

    timer.setTime(2.hours)

    assertThat(timer.timeLeft.value).isEqualTo(2.hours)
  }

  @Test
  fun `start sets isRunning to true`() = runTest {
    val timer = createTimer()
    timer.setTime(1.minutes)

    timer.start()

    assertThat(timer.isRunning.value).isTrue()
  }

  @Test
  fun `start does nothing when already running`() = runTest {
    val timer = createTimer()
    timer.setTime(1.minutes)
    timer.start()

    timer.setTime(2.minutes)
    timer.start() // Should not restart

    assertThat(timer.timeLeft.value).isEqualTo(2.minutes)
    advanceTimeBy(1001)
    assertThat(timer.timeLeft.value).isEqualTo(1.minutes + 59.seconds)
  }

  @Test
  fun `stop sets isRunning to false`() = runTest {
    val timer = createTimer()
    timer.setTime(1.minutes)
    timer.start()

    timer.stop()

    assertThat(timer.isRunning.value).isFalse()
  }

  @Test
  fun `stop cancels countdown`() = runTest {
    val timer = createTimer()
    timer.setTime(1.minutes)
    timer.start()
    advanceTimeBy(5001) // 5 seconds pass
    assertThat(timer.timeLeft.value).isEqualTo(55.seconds)

    timer.stop()
    advanceTimeBy(5001) // 5 more seconds pass

    assertThat(timer.timeLeft.value).isEqualTo(55.seconds) // Time should not have changed
  }

  @Test
  fun `timer counts down every second`() = runTest {
    val timer = createTimer()
    timer.setTime(5.seconds)
    timer.start()

    advanceTimeBy(1001)
    assertThat(timer.timeLeft.value).isEqualTo(4.seconds)

    advanceTimeBy(1001)
    assertThat(timer.timeLeft.value).isEqualTo(3.seconds)

    advanceTimeBy(1001)
    assertThat(timer.timeLeft.value).isEqualTo(2.seconds)
  }

  @Test
  fun `timer stops at zero`() = runTest {
    val timer = createTimer()
    timer.setTime(3.seconds)
    timer.start()

    advanceTimeBy(4001) // More than 3 seconds

    assertThat(timer.timeLeft.value).isEqualTo(0.seconds)
    assertThat(timer.isRunning.value).isFalse()
  }

  @Test
  fun `timer does not go below zero`() = runTest {
    val timer = createTimer()
    timer.setTime(2.seconds)
    timer.start()

    advanceTimeBy(10001) // Way more than 2 seconds

    assertThat(timer.timeLeft.value).isEqualTo(0.seconds)
  }

  @Test
  fun `can restart after completion`() = runTest {
    val timer = createTimer()
    timer.setTime(2.seconds)
    timer.start()
    advanceTimeBy(3001) // Let it complete
    assertThat(timer.isRunning.value).isFalse()

    timer.setTime(5.seconds)
    timer.start()

    assertThat(timer.isRunning.value).isTrue()
    assertThat(timer.timeLeft.value).isEqualTo(5.seconds)
  }

  @Test
  fun `can restart after stop`() = runTest {
    val timer = createTimer()
    timer.setTime(10.seconds)
    timer.start()
    advanceTimeBy(3001)
    timer.stop()

    timer.setTime(5.seconds)
    timer.start()

    assertThat(timer.isRunning.value).isTrue()
    advanceTimeBy(2001)
    assertThat(timer.timeLeft.value).isEqualTo(3.seconds)
  }

  private fun TestScope.createTimer(): TimerImpl = TimerImpl(this)
}
