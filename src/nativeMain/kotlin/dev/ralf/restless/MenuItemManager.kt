package dev.ralf.restless

interface MenuItemManager {
  fun updateTitle(title: String)

  fun setManualMode(isManual: Boolean)

  suspend fun setupMenu()
}
