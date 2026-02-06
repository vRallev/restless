package dev.ralf.restless

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy

@Inject
@SingleIn(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class Application(
  private val powerStatusMonitor: PowerStatusMonitor,
  private val lockStatusMonitor: LockStatusMonitor,
  private val menuItemManager: MenuItemManager,
  private val idleStatusMonitor: IdleStatusMonitor,
  private val rootPresenter: RootPresenter,
) {

  suspend fun run() = coroutineScope {
    menuItemManager.setupMenu()

    launch(Dispatchers.Main) { powerStatusMonitor.monitor() }
    launch(Dispatchers.Main) { lockStatusMonitor.monitor() }
    launch(Dispatchers.Main) { idleStatusMonitor.monitor(idleThreshold = 10.seconds) }

    val rootPresenterModels =
      launchMolecule(RecompositionMode.Immediate) { rootPresenter.present() }

    launch(Dispatchers.Main) {
      rootPresenterModels.collect { model ->
        menuItemManager.updateTitle(model.title)
        menuItemManager.setManualMode(model.isManualMode)
      }
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
fun main(): Unit = runBlocking {
  val app = NSApplication.sharedApplication
  app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyAccessory)

  val graph = createGraph<AppGraph>()
  launch(Dispatchers.IO) { graph.application.run() }

  app.run()
}
