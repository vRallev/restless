package dev.ralf.restless

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

/**
 * Integration tests for RootPresenter with real ModeController and Timer. Tests the full reactive
 * flow from state changes through presenter to model output.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RootPresenterIntegrationTest {

  @Test
  fun `initial state shows 00-00 in automatic mode`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        val model = awaitItem()
        assertThat(model.title).isEqualTo("00:00")
        assertThat(model.isManualMode).isFalse()
        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `automatic mode starts timer when conditions are met`() = runTest {
    val testEnv = createTestEnvironment()

    // Set conditions before starting the flow
    testEnv.powerMonitor.setOnCharger(true)
    testEnv.idleMonitor.setIdle(true)
    testEnv.lockMonitor.setLocked(false)

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        // Skip initial emissions and wait for LaunchedEffect to run
        skipItems(1)
        advanceTimeBy(100)

        val model = expectMostRecentItem()
        assertThat(model.title).contains("02:00")
        assertThat(model.isManualMode).isFalse()
        assertThat(testEnv.caffeinate.startCalled).isTrue()
        assertThat(testEnv.caffeinate.lastDuration).isEqualTo(2.hours)

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `automatic mode stops timer when charger disconnected`() = runTest {
    // Start with conditions met
    val testEnv =
      createTestEnvironment(initialOnCharger = true, initialIdle = true, initialLocked = false)

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        skipItems(1)
        advanceTimeBy(100)

        // Verify timer started
        var model = expectMostRecentItem()
        assertThat(model.title).contains("02:00")

        // Disconnect charger
        testEnv.powerMonitor.setOnCharger(false)
        advanceTimeBy(100)

        model = expectMostRecentItem()
        assertThat(model.title).isEqualTo("00:00")
        assertThat(testEnv.caffeinate.stopCalled).isTrue()

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `manual mode click adds 1 hour`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem() // Initial state

        // Click to enter manual mode
        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)

        val model = expectMostRecentItem()
        assertThat(model.title).contains("01:00")
        assertThat(model.isManualMode).isTrue()

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `manual mode multiple clicks add hours`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem() // Initial state

        // Click 3 times
        testEnv.modeController.onStatusBarClick()
        testEnv.modeController.onStatusBarClick()
        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)

        val model = expectMostRecentItem()
        assertThat(model.title).contains("03:00")
        assertThat(model.isManualMode).isTrue()

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `manual mode ignores system state changes`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem() // Initial state

        // Enter manual mode
        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)

        // Change all system states
        testEnv.powerMonitor.setOnCharger(false)
        testEnv.idleMonitor.setIdle(false)
        testEnv.lockMonitor.setLocked(true)
        advanceTimeBy(100)

        // Timer should still show 1 hour
        val model = expectMostRecentItem()
        assertThat(model.title).contains("01:00")
        assertThat(model.isManualMode).isTrue()

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `cancel returns to automatic mode`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem() // Initial state

        // Enter manual mode
        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)
        assertThat(expectMostRecentItem().isManualMode).isTrue()

        // Cancel
        testEnv.modeController.cancelManualMode()
        advanceTimeBy(100)

        val model = expectMostRecentItem()
        assertThat(model.title).isEqualTo("00:00")
        assertThat(model.isManualMode).isFalse()

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `timer counts down over time`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem() // Initial state

        // Enter manual mode with 1 hour
        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)

        // Advance 1 minute (60 seconds + buffer)
        advanceTimeBy(60_001)

        val model = expectMostRecentItem()
        assertThat(model.title).contains("00:59")

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `manual mode caps at 24 hours`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem() // Initial state

        // Click 30 times (should cap at 24)
        repeat(30) { testEnv.modeController.onStatusBarClick() }
        advanceTimeBy(100)

        val model = expectMostRecentItem()
        assertThat(model.title).contains("24:00")

        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `caffeinate updates on each manual click`() = runTest {
    val testEnv = createTestEnvironment()

    moleculeFlow(RecompositionMode.Immediate) { testEnv.presenter.present() }
      .test {
        awaitItem()

        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)
        assertThat(testEnv.caffeinate.lastDuration).isEqualTo(1.hours)

        testEnv.modeController.onStatusBarClick()
        advanceTimeBy(100)
        assertThat(testEnv.caffeinate.lastDuration).isEqualTo(2.hours)

        cancelAndIgnoreRemainingEvents()
      }
  }

  private fun TestScope.createTestEnvironment(
    initialOnCharger: Boolean = false,
    initialIdle: Boolean = false,
    initialLocked: Boolean = false,
  ): TestEnvironment {
    val powerMonitor = FakePowerStatusMonitor(initialOnCharger)
    val idleMonitor = FakeIdleStatusMonitor(initialIdle)
    val lockMonitor = FakeLockStatusMonitor(initialLocked)
    val caffeinate = FakeCaffeinate()
    val timer = TimerImpl(this)
    val modeController = ModeControllerImpl(timer, caffeinate)

    val presenter =
      RootPresenter(
        powerStatusMonitor = powerMonitor,
        idleStatusMonitor = idleMonitor,
        lockStatusMonitor = lockMonitor,
        timer = timer,
        caffeinate = caffeinate,
        modeController = modeController,
      )

    return TestEnvironment(
      presenter = presenter,
      modeController = modeController,
      caffeinate = caffeinate,
      powerMonitor = powerMonitor,
      idleMonitor = idleMonitor,
      lockMonitor = lockMonitor,
    )
  }

  private class TestEnvironment(
    val presenter: RootPresenter,
    val modeController: ModeControllerImpl,
    val caffeinate: FakeCaffeinate,
    val powerMonitor: FakePowerStatusMonitor,
    val idleMonitor: FakeIdleStatusMonitor,
    val lockMonitor: FakeLockStatusMonitor,
  )
}
