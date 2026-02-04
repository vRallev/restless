package dev.ralf.screen

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ModeControllerImplTest {

  @Test
  fun `initial mode is AUTOMATIC`() {
    val controller = createController()

    assertThat(controller.mode.value).isEqualTo(AppMode.AUTOMATIC)
  }

  @Test
  fun `clicking switches to MANUAL mode`() {
    val controller = createController()

    controller.onStatusBarClick()

    assertThat(controller.mode.value).isEqualTo(AppMode.MANUAL)
  }

  @Test
  fun `clicking adds 1 hour to timer`() {
    val fakeTimer = FakeTimer()
    val controller = createController(timer = fakeTimer)

    controller.onStatusBarClick()

    assertThat(fakeTimer.timeLeft.value).isEqualTo(1.hours)
  }

  @Test
  fun `clicking starts the timer`() {
    val fakeTimer = FakeTimer()
    val controller = createController(timer = fakeTimer)

    controller.onStatusBarClick()

    assertThat(fakeTimer.isRunning.value).isTrue()
  }

  @Test
  fun `clicking starts caffeinate`() {
    val fakeCaffeinate = FakeCaffeinate()
    val controller = createController(caffeinate = fakeCaffeinate)

    controller.onStatusBarClick()

    assertThat(fakeCaffeinate.startCalled).isTrue()
    assertThat(fakeCaffeinate.lastDuration).isEqualTo(1.hours)
  }

  @Test
  fun `multiple clicks add hours`() {
    val fakeTimer = FakeTimer()
    val controller = createController(timer = fakeTimer)

    controller.onStatusBarClick()
    controller.onStatusBarClick()
    controller.onStatusBarClick()

    assertThat(fakeTimer.timeLeft.value).isEqualTo(3.hours)
  }

  @Test
  fun `clicking caps timer at 24 hours`() {
    val fakeTimer = FakeTimer(initialTime = 23.hours + 30.minutes)
    val controller = createController(timer = fakeTimer)

    controller.onStatusBarClick()

    assertThat(fakeTimer.timeLeft.value).isEqualTo(24.hours)
  }

  @Test
  fun `clicking when already at 24 hours stays at 24 hours`() {
    val fakeTimer = FakeTimer(initialTime = 24.hours)
    val controller = createController(timer = fakeTimer)

    controller.onStatusBarClick()

    assertThat(fakeTimer.timeLeft.value).isEqualTo(24.hours)
  }

  @Test
  fun `cancel returns to AUTOMATIC mode`() {
    val controller = createController()
    controller.onStatusBarClick() // Switch to manual
    assertThat(controller.mode.value).isEqualTo(AppMode.MANUAL)

    controller.cancelManualMode()

    assertThat(controller.mode.value).isEqualTo(AppMode.AUTOMATIC)
  }

  @Test
  fun `cancel stops the timer`() {
    val fakeTimer = FakeTimer()
    val controller = createController(timer = fakeTimer)
    controller.onStatusBarClick()

    controller.cancelManualMode()

    assertThat(fakeTimer.stopCalled).isTrue()
  }

  @Test
  fun `cancel resets timer to zero`() {
    val fakeTimer = FakeTimer()
    val controller = createController(timer = fakeTimer)
    controller.onStatusBarClick()
    controller.onStatusBarClick()

    controller.cancelManualMode()

    assertThat(fakeTimer.timeLeft.value).isEqualTo(Duration.ZERO)
  }

  @Test
  fun `cancel stops caffeinate`() {
    val fakeCaffeinate = FakeCaffeinate()
    val controller = createController(caffeinate = fakeCaffeinate)
    controller.onStatusBarClick()

    controller.cancelManualMode()

    assertThat(fakeCaffeinate.stopCalled).isTrue()
  }

  @Test
  fun `clicking updates caffeinate with new duration`() {
    val fakeCaffeinate = FakeCaffeinate()
    val controller = createController(caffeinate = fakeCaffeinate)

    controller.onStatusBarClick()
    controller.onStatusBarClick()

    assertThat(fakeCaffeinate.lastDuration).isEqualTo(2.hours)
  }

  private fun createController(
    timer: Timer = FakeTimer(),
    caffeinate: Caffeinate = FakeCaffeinate(),
  ) = ModeControllerImpl(timer, caffeinate)
}
